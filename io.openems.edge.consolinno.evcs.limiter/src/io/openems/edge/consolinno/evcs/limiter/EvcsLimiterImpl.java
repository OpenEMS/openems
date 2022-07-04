package io.openems.edge.consolinno.evcs.limiter;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.timer.TimerByCycles;
import io.openems.edge.common.timer.TimerByTime;
import io.openems.edge.common.timer.TimerType;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.core.timer.TimerHandler;
import io.openems.edge.core.timer.TimerHandlerImpl;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.time.Instant;

/**
 * This provides a Limiter for the EVCS.
 * This Limiter Checks: 1: the Power per phase. Based on that, it limits the power of the appropriate EVCS to prevent an unbalanced load.
 * It will also prevent a blackout by limiting the phases.
 * 2: the overall Power Consumption and limits it based on the config
 * Furthermore the Limiter is capable to smooth out its reallocation process by:
 * a) Offsetting the reallocation based on time
 * b) Only reallocates if there is at least x Amperage free
 * c) Reallocates up to e.g. 100 A, but only limits when there is a 102 A load
 * An individual Evcs can also be configured to be of higher priority and thus won't be limited unless it absolutely has to.
 * Then all high priority Evcs will share a configured amount of Amperage.
 * The Limiter is also capable of Offsetting its Calculations based on Configured Meters. So the Powerlimit can be determined
 * based on e.g. a PV Meter and the Consumption Meter of a House.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "Evcs.Limiter", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE, property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class EvcsLimiterImpl extends AbstractOpenemsComponent implements OpenemsComponent, EvcsLimiterPower, EventHandler {

    @Reference
    TimerByTime timerByTime;

    @Reference
    TimerByCycles timerByCycles;


    private final Logger log = LoggerFactory.getLogger(EvcsLimiterImpl.class);
    private String[] ids;
    private ManagedEvcs[] evcss;
    private List<ManagedEvcs> active = new ArrayList<>();
    private List<ManagedEvcs> priorityList = new ArrayList<>();
    private List<ManagedEvcs> nonPriorityList = new ArrayList<>();
    private int priorityAmount;
    private int nonPriorityAmount;
    private Integer currentL1;
    private Integer currentL2;
    private Integer currentL3;
    private Integer evcsL1;
    private Integer evcsL2;
    private Integer evcsL3;
    //ID and the Last known Power Request of an EVCS that was turned off.
    private final Map<String, EvcsOnHold> powerWaitingList = new HashMap<>();
    //The Maximum Power Consumptions
    private int max;
    //The phases where the maximal Consumption is on
    private int maxIndex;
    //The middle Load. Can be the same as max
    private int middle;
    //The phase where the middle Consumption is on
    private int middleIndex;
    //The Index of middle if middleIndex==maxIndex
    private int middleIndex2;
    //The Minimum Power Consumption
    private int min;
    //The phases with the minimal Consumption
    private int minIndex;
    private int min2Index;
    private static final int MINIMUM_CURRENT = 6;
    private static final int MINIMUM_POWER = 1380;
    private static int GRID_VOLTAGE;
    private static final int ONE_PHASE_INDEX = 0;
    private static final int TWO_PHASE_INDEX = 1;
    private static final int ONE_PHASE_INDEX_2 = 2;
    private static final int TWO_PHASE_INDEX_2 = 3;
    private static final int OFF = 0;
    private int phaseLimit;
    private int powerLimit;
    private AsymmetricMeter meter;
    private AsymmetricMeter symmetryMeter;
    private static final int MAX_SECONDS_BEFORE_SYMMETRY = 55;
    private TimerHandler time;
    private static final String SYMMETRY_SPEED = "cycleOffset";
    private static final String MAXIMUM_ALLOWED_TIME = "guard";
    private static final String SMOOTH_TIME = "smooth";
    private static final String ONE_MINUTE = "minute";

    private int l1Offs;
    private int l2Offs;
    private int l3Offs;

    private List<Integer> smoothAverageList = new ArrayList<>();

    private Config config;

    private boolean swapped;

    @Reference
    ComponentManager cpm;



    public EvcsLimiterImpl() {
        super(OpenemsComponent.ChannelId.values(), EvcsLimiterPower.ChannelId.values());
    }

    @Activate
    void activate(ComponentContext context, Config config) throws ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);

    }

    @Modified
    void modified(ComponentContext context, Config config) throws ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }

    /**
     * Applies the Configuration when the component activates or is being modified.
     *
     * @param config Apache Felix config
     * @throws ConfigurationException When the Evcs Array is wrong or the Timer is not existent.
     */
    private void activateOrModifiedRoutine(Config config) throws ConfigurationException {
        this.config = config;
        GRID_VOLTAGE = config.grid().getValue();
        if (this.config.useMeter() && !this.checkAndSetConfiguredMeter(config.meter(), config.symmetryMeter())) {
            this.log.error("The configured Meter is not active or not an Asymmetric Meter.");
        }
        this.ids = config.evcss();
        this.evcss = new ManagedEvcs[this.ids.length];
        this.active = Arrays.asList(this.evcss.clone());
        this.phaseLimit = config.phaseLimit() * GRID_VOLTAGE;
        this.powerLimit = config.powerLimit();
        try {
            this.time = new TimerHandlerImpl(super.id(),this.timerByTime,this.timerByCycles);
            this.time.addOneIdentifier(SYMMETRY_SPEED, TimerType.CYCLES, this.config.symmetryOffset());
            this.time.addOneIdentifier(MAXIMUM_ALLOWED_TIME, TimerType.TIME, MAX_SECONDS_BEFORE_SYMMETRY);
            this.time.addOneIdentifier(SMOOTH_TIME, TimerType.TIME, (this.config.deltaTime() * 60) + 1);
            this.time.addOneIdentifier(ONE_MINUTE, TimerType.TIME, 60);
        } catch (ConfigurationException e) {
            this.log.error("Couldn't find Timer. Check Config!");
        }

        this.updateEvcss();
    }

    /**
     * Checks if the Connected Meter is an AsymmetricESS.
     *
     * @param meter  Id of the Configured Meter.
     * @param meter2 Id of the Meter used for symmetry.
     * @return true if AsymmetricEss
     */
    private boolean checkAndSetConfiguredMeter(String meter, String meter2) {

        try {
            OpenemsComponent component = this.cpm.getComponent(meter);
            if (component instanceof AsymmetricMeter) {
                this.meter = (AsymmetricMeter) component;
                if (meter2 == null || meter2.equals("")) {
                    this.symmetryMeter = (AsymmetricMeter) component;
                } else {
                    OpenemsComponent symmetryComponent = this.cpm.getComponent(meter2);
                    if (symmetryComponent instanceof AsymmetricMeter) {
                        this.symmetryMeter = (AsymmetricMeter) symmetryComponent;
                    }

                }
                return true;
            }

        } catch (OpenemsError.OpenemsNamedException e) {
            return false;
        }
        return false;
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }

    @Override
    public String debugLog() {
        ManagedEvcs[] all = this.getEvcs();
        List<String> active = new ArrayList<>();
        for (ManagedEvcs managedEvcs : all) {
            active.add(managedEvcs.id());
        }

        return "L1: Total: " + this.currentL1 + " Evcs: " + this.evcsL1 + " | L2: Total: " + this.currentL2 + " Evcs: " + this.evcsL2 + " | L3: Total: " + this.currentL3 + " Evcs: " + this.evcsL3 + "| on " //
                + active.size() + "(" + (this.evcss.length - active.size()) + " waiting)" //
                + " EVCS: " + active;
    }

    @Override
    public void handleEvent(Event event) {
        if (this.evcss[0] == null) {
            try {
                this.updateEvcss();
            } catch (ConfigurationException e) {
                this.log.error("EVCS given are not EVCS.");
            }
        } else {
            //------Checks if the Config asks for a Meter and is valid------\\
            if (this.config.useMeter()) {
                if (this.meter == null || this.symmetryMeter == null || !this.config.symmetryMeter().equals("") && (!this.config.symmetryMeter().equals(this.config.meter()) && this.symmetryMeter == this.meter)) {
                    this.checkAndSetConfiguredMeter(this.config.meter(), this.config.symmetryMeter());
                } else {
                    this.updatePowerLimit();
                }
            }
            //-----Reallocate Resources------\\
            try {
                this.preLimiterRoutine();
            } catch (Exception ignored) {
                this.log.error("Couldn't complete pre limiter routine. This should not have happened.");
            }

            //-----Check if the power has to be limited-----\\
            this.limiterRoutine();
        }
    }

    /**
     * Reallocates free resources and manages the waiting list (if necessary).
     * The Routine tries to:
     * 1. Get Evcs that are turned off to turn on again
     * 2. If one Evcs waits for long enough -> swap it with the longest running one
     * 3. Update internal variables
     * 4. Reallocate free resources to the high priority Evcs (if existent)
     * 5. Reallocate free resources to everyone else
     */
    private void preLimiterRoutine() {
        this.checkWaitingList();
        this.swapWaitingEvcs();
        this.getActiveEvcss();
        this.updatePower(true);
        this.reallocateToPriority();
        this.updatePower(true);
        this.reallocateFreeResources();
        this.updatePower(true);
        this.getActiveEvcss();
    }

    /**
     * Balances the Loads, and checks for Phase and PowerLimit.
     */
    private void limiterRoutine() {
        if (this.getPowerLimitValue() > 0) {
            this.powerLimit = getPowerLimitValue();
        }
        this.checkForUnbalancedLoad();
        this.updatePower(true);
        this.checkPhaseLimit();
        this.updatePower(true);
        this.checkPowerLimit();
        this.updatePower(true);
        this.updateChannel();
        this.getActiveEvcss();
    }

    /**
     * Checks if an unbalanced Load exists, and corrects it if necessary.
     */
    private void checkForUnbalancedLoad() {
        Optional<List<ManagedEvcs[]>> problem;
        problem = this.getRequestedPower();
        if (problem.isPresent()) {
            try {
                //Offsets the Symmetry Check if it was configured to wait
                if (this.time.checkTimeIsUp(SYMMETRY_SPEED) || this.time.checkTimeIsUp(MAXIMUM_ALLOWED_TIME)) {
                    this.limitPower(problem.get());
                    this.time.resetTimer(SYMMETRY_SPEED);
                    this.time.resetTimer(MAXIMUM_ALLOWED_TIME);
                }

            } catch (Exception e) {
                this.log.warn("Unable to Limit Power without turning an EVCS off!"
                        + " One or more EVCS will now be turned off for " + this.config.offTime() + " minutes.");
                try {
                    this.turnOffEvcsBalance();
                } catch (Exception emergencyStop) {
                    this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                    this.emergencyStop();
                }
            }
        } else {
            if (this.config.symmetryOffset() > 0) {
                this.time.resetTimer(SYMMETRY_SPEED);
                this.time.resetTimer(MAXIMUM_ALLOWED_TIME);
            }
        }
    }

    /**
     * Checks if the EVCS Cluster pulls more A from the Grid then is allowed and corrects it to prevent a black-out.
     */
    private void checkPhaseLimit() {

        if (this.phaseLimit != 0 && this.getMaximumLoad() > (this.phaseLimit / GRID_VOLTAGE) + this.config.phaseTolerance()) {
            this.log.info("Phase Limit has been exceeded. Rectifying in Process...");
            try {
                this.applyPhaseLimit();
            } catch (EvcsException | OpenemsError.OpenemsNamedException e) {
                this.log.warn("Unable to apply Phase Limit without turning an EVCS off!"
                        + " One or more EVCS will now be turned off for " + this.config.offTime() + " minutes.");
                try {
                    this.turnOffEvcsPhaseLimit();
                } catch (Exception emergencyStop) {
                    this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                    this.emergencyStop();
                }
            }
        }
    }

    /**
     * Checks if the determined Power Limit has been exceeded and corrects it if necessary.
     */
    private void checkPowerLimit() {
        if (this.powerLimit != 0
                && (this.currentL1 + this.currentL2 + this.currentL3 >= (this.powerLimit / GRID_VOLTAGE) + this.config.powerTolerance())) {
            this.log.info("Power Limit has been exceeded. Rectifying in Process...");
            try {
                this.applyPowerLimit();
            } catch (EvcsException | OpenemsError.OpenemsNamedException e) {
                this.log.warn("Unable to apply Power Limit without turning an EVCS off!"
                        + " One or more EVCS will now be turned off for " + this.config.offTime() + " minutes.");
                try {
                    this.turnOffEvcsPowerLimit();
                } catch (Exception emergencyStop) {
                    this.log.error("Unable to Limit Power. All EVCS will now be turned off.");
                    this.emergencyStop();
                }
            }
        }
    }


    //----------------------Limit Methods------------------------\\


    //-------------Methods for Power Limiting------------\\

    /**
     * Evenly Reduces the power of all EVCS (if Possible).
     *
     * @throws EvcsException If the Power Limit can't be reached be limitation alone, some Evcs have to be turned off
     */
    private void applyPowerLimit() throws EvcsException, OpenemsError.OpenemsNamedException {

        //------Determine how much power needs to be reduced to be under the Power Limit------\\
        int powerToReduce = ((this.currentL1 + this.currentL2 + this.currentL3) - (this.powerLimit / GRID_VOLTAGE)) + 1;
        int priorityAmount = this.priorityAmount;
        int powerPerEvcs = 0;
        //Filter out the priority Evcs
        if (priorityAmount < this.active.size()) {
            //Calculate how much Power has to be reduced per Evcs
            powerPerEvcs = powerToReduce / (this.active.size() - priorityAmount);
        }
        ManagedEvcs[] activeArray = this.active.toArray(new ManagedEvcs[0]);
        for (int i = 0; i < activeArray.length; i++) {

            if (activeArray[i].getIsPriority().get()
                    && this.isEvcsActive(activeArray[i]) && this.nonPriorityAmount > 0) {
                continue;
            }
            //-----Calculate the new Power of an Evcs by subtracting the Reduce-Amount from the Charge Power-----\\
            int newPower = (activeArray[i].getChargePower().get() / GRID_VOLTAGE) - powerPerEvcs;
            int minPower = this.minPower(activeArray[i]);
            //----Validate the new Power----\\
            if (newPower > OFF && newPower > minPower) {
                activeArray[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                powerToReduce -= powerPerEvcs;
                this.log.info(activeArray[i].id() + " was reduced by " + powerPerEvcs * GRID_VOLTAGE //
                        + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        int previousPowerToReduce = powerToReduce;
        //----Checks if the Power Sum is under the Power Limit again-----\\
        //In case some Evcs can not be reduced (enough) some Evcs have to be reduced again
        while (powerToReduce > OFF) {
            powerToReduce = this.applyPowerLimit(powerToReduce, activeArray);
            if (powerToReduce != previousPowerToReduce) {
                previousPowerToReduce = powerToReduce;
            } else {
                //If the Power Limit can't be reached be limitation alone, some Evcs have to be turned off
                throw new EvcsPowerLimitException();
            }
        }

    }

    /**
     * This is a recursive Helper method of the above method. Should only be called from it and not externally!
     * Validates the newPower and returns PowerThatShouldBeReduced - PowerThatWasActuallyRemoved.
     *
     * @param powerToReduce The remaining power that has to be reduced
     * @param activeArray   The active Managed Evcs in an array
     * @return modified PowerToReduce
     * @throws OpenemsError.OpenemsNamedException This should not happen
     */
    private int applyPowerLimit(int powerToReduce, ManagedEvcs[] activeArray) throws OpenemsError.OpenemsNamedException {
        if (powerToReduce == 0) {
            powerToReduce = 1;
        }
        int powerPerEvcs = 0;
        if (this.priorityAmount < this.active.size()) {
            powerPerEvcs = powerToReduce / (this.active.size() - this.priorityAmount);
        }
        if (powerPerEvcs == 0) {
            powerPerEvcs = 1;
        }
        int newPower;

        for (ManagedEvcs managedEvcs : activeArray) {

            if (managedEvcs.getIsPriority().get() && this.isEvcsActive(managedEvcs) && this.nonPriorityAmount > 0) {
                continue;
            }

            if (managedEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    managedEvcs.getSetChargePowerLimitChannel().value().orElse(0)) != 0) {
                newPower = ((managedEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        managedEvcs.getSetChargePowerLimitChannel().value().orElse(0)))//
                        / GRID_VOLTAGE) - powerPerEvcs;
            } else {
                newPower = (managedEvcs.getChargePower().get() / GRID_VOLTAGE) - powerPerEvcs;
            }
            int minPower = this.minPower(managedEvcs);
            if (newPower > OFF && newPower >= minPower) {
                managedEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                powerToReduce -= powerPerEvcs;
                this.log.info(managedEvcs.id() + " was reduced by " + powerPerEvcs * GRID_VOLTAGE
                        + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        return powerToReduce;
    }

    /**
     * Checks if a given evcss is on the active list.
     *
     * @param evcss Evcss that has to be checked.
     * @return True if its on the list.
     */
    private boolean isEvcsActive(ManagedEvcs evcss) {
        return this.active.contains(evcss);
    }

    //---------------Methods for Phase Limiting--------------\\

    /**
     * Applies the Limit for the Phases, specified in the Config.
     * Priority List:
     * 1. All Three Phases are over the Limit:
     * 1.1 Three Phasers
     * 1.2. Two Phasers IF Three Phasers don't exist / not enough to reduce at least one Phase
     * 1.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 2. Two phases are over the Limit:
     * 2.1 Two Phasers
     * 2.2 Three Phasers IF no Two Phasers exist / not enough to reduce at least one Phase
     * 2.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 3. One Phases is over the Limit:
     * 3.1 One Phasers
     * 3.2 Three Phasers IF no One Phasers exist / not enough to reduce at least one Phase
     * 3.3 Two Phasers IF neither Three Phasers nor One Phasers exist / not enough to reduce at least one Phase
     *
     * @throws EvcsException If its unable to reduce the phases without Turning an EVCS off
     */
    private void applyPhaseLimit() throws EvcsException, OpenemsError.OpenemsNamedException {
        //TODO this is unbelievably ugly and needs to be changed.
        //What phases are causing the problems
        List<Integer> problemPhases = new ArrayList<>();
        int afterOnePhaseReduction;
        int afterTwoPhaseReduction;
        int afterThreePhaseReduction;

        this.isProblem(this.currentL1,problemPhases,1);
        this.isProblem(this.currentL2,problemPhases,2);
        this.isProblem(this.currentL2,problemPhases,3);

        //Get all evcs on that phase
        // One Phase and two phase are sorted by Phase
        List<ManagedEvcs[]> onePhase = new ArrayList<>();
        //0=All that are on phase 1 and 2;1= All that are on Phase 2 and Three;2= All that are on phase 3 and 1
        List<ManagedEvcs[]> twoPhase = new ArrayList<>();
        //Three phases are already on all of them so there is only one Array
        ManagedEvcs[] threePhase;
        threePhase = this.getThreePhaseEvcs();
        for (int i = 0; i < problemPhases.size(); i++) {
            onePhase.add(i, this.getOnePhaseEvcs(problemPhases.get(i)));
            if (i != 3) {
                twoPhase.add(i, this.getTwoPhaseEvcs(problemPhases.get(i), i - 1));
            }
        }

        //Get the Value that has to be reduced
        int reduceL1 = this.currentL1 - (this.phaseLimit / GRID_VOLTAGE);
        int reduceL2 = this.currentL2 - (this.phaseLimit / GRID_VOLTAGE);
        int reduceL3 = this.currentL3 - (this.phaseLimit / GRID_VOLTAGE);
        int amountPerEvcs;
        //The minReduce will be used to break out of the while loops if there is no further limitation possible
        int minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
        //Actual limiting
        int previousReduceAmount = minReduce;
        if (threePhase.length > 1) {
            while (this.threePhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

                //1.1
                //The Three Phase EVCS will be reduced until at least one is under the limit
                afterThreePhaseReduction = (this.reduceThreePhaseEvcs(threePhase, minReduce));
                reduceL1 -= afterThreePhaseReduction;
                reduceL2 -= afterThreePhaseReduction;
                reduceL3 -= afterThreePhaseReduction;
                minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
                if (minReduce != previousReduceAmount) {
                    previousReduceAmount = minReduce;
                } else {
                    //if the three Phasers cant be reduced
                    break;
                }
            }
        } else {
            int phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce) - 1;
            //1.2
            //reduce at least one phase
            if (onePhase.size() > 0) {
                while (minReduce > 0) {
                    amountPerEvcs = minReduce / onePhase.get(phaseIndex).length;
                    minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex),//
                            amountPerEvcs, minReduce);

                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        break;
                    }
                }
            } else {
                while (minReduce > 0) {
                    int phaseIndex2;
                    if (phaseIndex == 0) {
                        phaseIndex2 = 3;
                    } else {
                        phaseIndex2 = phaseIndex - 1;
                    }
                    int twoPhaseAmount = (twoPhase.get(phaseIndex)).length + twoPhase.get(phaseIndex2).length;
                    amountPerEvcs = minReduce / twoPhaseAmount;
                    int reducedByPhase1 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex),//
                            amountPerEvcs);
                    int reducedByPhase2 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex2),//
                            amountPerEvcs);
                    minReduce -= (reducedByPhase1 + reducedByPhase2) / 2;
                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        break;
                    }
                }
            }
        }
        //1.3
        //The Two Phase EVCS will be reduced until at least one is under the limit
        if (this.twoPhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {
            //Get what phase is ok
            int phaseOkIndex = this.getOnePhaseUnderPhaseLimit(reduceL1, reduceL2);
            int phaseIndex;
            switch (phaseOkIndex) {
                case 1:
                    minReduce = Math.min(reduceL2, reduceL3);
                    break;
                case 2:
                    minReduce = Math.min(reduceL1, reduceL3);
                    break;
                case 3:
                    minReduce = Math.min(reduceL1, reduceL2);
                    break;
            }
            ManagedEvcs[] twoPhaseOverLimit;
            if (phaseOkIndex == 3) {
                twoPhaseOverLimit = twoPhase.get(0);
            } else {
                twoPhaseOverLimit = twoPhase.get(phaseOkIndex);
            }
            if (twoPhaseOverLimit.length > 0) {
                //2.1
                //reduce until one phase is under the limit
                previousReduceAmount = minReduce;
                while (minReduce > 0) {
                    afterTwoPhaseReduction = this.reduceTwoPhaseEvcs(//
                            twoPhaseOverLimit, minReduce);
                    switch (phaseOkIndex) {
                        case 1:
                            reduceL2 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;

                            break;
                        case 2:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL3 -= afterTwoPhaseReduction;

                            break;
                        case 3:
                            reduceL1 -= afterTwoPhaseReduction;
                            reduceL2 -= afterTwoPhaseReduction;

                            break;
                    }

                    minReduce -= afterTwoPhaseReduction / 2;

                    if (minReduce != previousReduceAmount) {
                        previousReduceAmount = minReduce;
                    } else {
                        break;
                    }
                }
            } else {
                phaseIndex = this.getPhaseByPower(reduceL1, reduceL2, reduceL3, minReduce) - 1;
                //2.2
                //reduce at least one phase
                if (onePhase.size() > 0) {
                    while (minReduce > 0) {
                        amountPerEvcs = minReduce / onePhase.get(phaseIndex).length;
                        minReduce = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex),//
                                amountPerEvcs, minReduce);

                        if (minReduce != previousReduceAmount) {
                            previousReduceAmount = minReduce;
                        } else {
                            break;
                        }
                    }
                } else {
                    while (this.threePhasesOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

                        //2.3
                        //The Three Phase EVCS will be reduced until at least one is under the limit
                        afterThreePhaseReduction = this.reduceThreePhaseEvcs(threePhase, minReduce);
                        reduceL1 -= afterThreePhaseReduction;
                        reduceL2 -= afterThreePhaseReduction;
                        reduceL3 -= afterThreePhaseReduction;
                        minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
                        if (minReduce != previousReduceAmount) {
                            previousReduceAmount = minReduce;
                        } else {
                            //if the three Phasers cant be reduced
                            break;
                        }
                    }
                }
            }
        }
        //3.1
        //The one Phase EVCS will be reduces until the last phase is under the limit
        if (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

            int phaseIndex = 0;
            int problemOnePhase = 1;
            if (onePhase.size() > 0) {
                switch (this.getTwoPhasesUnderPhaseLimit(reduceL1, reduceL2, reduceL3)) {
                    case 1:
                        if (onePhase.get(0).length == 0){
                            amountPerEvcs = reduceL1;
                        }
                        amountPerEvcs = reduceL1 / onePhase.get(0).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(0),//
                                amountPerEvcs, reduceL1);
                        reduceL1 = afterOnePhaseReduction;
                        problemOnePhase = reduceL1;
                        break;
                    case 2:
                        if (onePhase.get(1).length == 0){
                            amountPerEvcs = reduceL2;
                        }
                        amountPerEvcs = reduceL2 / onePhase.get(1).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(1),//
                                amountPerEvcs, reduceL2);
                        reduceL2 = afterOnePhaseReduction;
                        phaseIndex = 1;
                        problemOnePhase = reduceL2;
                        break;
                    case 3:
                        if (onePhase.get(2).length == 0){
                            amountPerEvcs = reduceL3;
                        }
                        amountPerEvcs = reduceL3 / onePhase.get(2).length;
                        afterOnePhaseReduction = this.reduceOnePhaseEvcs(onePhase.get(2),//
                                amountPerEvcs, reduceL3);
                        reduceL3 = afterOnePhaseReduction;
                        phaseIndex = 2;
                        problemOnePhase = reduceL3;
                        break;
                }
                if (problemOnePhase <= 0) {
                    this.log.info("Successfully applied Phase limit");
                } else {
                    previousReduceAmount = problemOnePhase;
                    while (problemOnePhase > 0) {
                        amountPerEvcs = problemOnePhase / onePhase.get(phaseIndex).length;
                        problemOnePhase = this.reduceOnePhaseEvcs(onePhase.get(phaseIndex),//
                                amountPerEvcs, problemOnePhase);

                        if (problemOnePhase != previousReduceAmount) {
                            previousReduceAmount = problemOnePhase;
                        } else {
                            throw new EvcsPhaseLimitException();
                        }
                    }
                }


            } else {
                if (threePhase.length > 1) {
                    while (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {

                        //3.2
                        //The Three Phase EVCS will be reduced until at least one is under the limit
                        afterThreePhaseReduction = this.reduceThreePhaseEvcs(threePhase, minReduce);
                        reduceL1 -= afterThreePhaseReduction;
                        reduceL2 -= afterThreePhaseReduction;
                        reduceL3 -= afterThreePhaseReduction;
                        minReduce = Math.min(Math.min(reduceL1, reduceL2), reduceL3);
                        if (minReduce != previousReduceAmount) {
                            previousReduceAmount = minReduce;
                        } else {
                            //if the three Phasers cant be reduced
                            break;
                        }
                    }
                    //3.3
                } else if (twoPhase.size() > 1) {
                    while (this.onePhaseOverPhaseLimit(reduceL1, reduceL2, reduceL3)) {
                        phaseIndex = this.getTwoPhasesUnderPhaseLimit(reduceL1, reduceL2, reduceL3);
                        int phaseIndex2;
                        if (phaseIndex == 0) {
                            phaseIndex2 = 3;
                        } else {
                            phaseIndex2 = phaseIndex - 1;
                        }
                        int twoPhaseAmount = (twoPhase.get(phaseIndex)).length + twoPhase.get(phaseIndex2).length;
                        amountPerEvcs = minReduce / twoPhaseAmount;
                        int reducedByPhase1 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex),//
                                amountPerEvcs);
                        int reducedByPhase2 = minReduce - this.reduceTwoPhaseEvcs(twoPhase.get(phaseIndex2),//
                                amountPerEvcs);
                        minReduce -= (reducedByPhase1 + reducedByPhase2) / 2;
                        if (minReduce != previousReduceAmount) {
                            previousReduceAmount = minReduce;
                        } else {
                            break;
                        }

                    }
                }
                if (reduceL1 > 0 || reduceL2 > 0 || reduceL3 > 0) {
                    throw new EvcsPhaseLimitException();
                }
            }
        }
    }

    /**
     * Checks if a Phase is unbalanced.
     * @param givenPhase Current on a Phase
     * @param problemPhases array that contain all Phases that are problematic
     * @param i index of that phase
     */
    private void isProblem(Integer givenPhase, List<Integer> problemPhases, int i) {
        if (givenPhase > (this.phaseLimit / GRID_VOLTAGE)) {
            problemPhases.add(i);
        }
    }


    /**
     * Checks if only One Phase is over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean onePhaseOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0 && reduceL2 <= 0 && reduceL3 <= 0) {
            return true;
        } else if (reduceL1 <= 0 && reduceL2 > 0 && reduceL3 <= 0) {
            return true;
        } else {
            return reduceL1 <= 0 && reduceL2 <= 0 && reduceL3 > 0;
        }
    }

    /**
     * Returns the Phase that is the only one that is still over the Phase Limit.
     * NOTE: Only call after check has been done that only one if over the limit in the first place.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return Index of the Phase that is over the Limit
     */
    private int getTwoPhasesUnderPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0 && reduceL2 <= 0 && reduceL3 <= 0) {
            return 1;
        } else if (reduceL1 <= 0 && reduceL2 > 0 && reduceL3 <= 0) {
            return 2;
        } else {
            return 3;
        }

    }

    /**
     * Checks if two Phases are over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean twoPhasesOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        if (reduceL1 > 0) {
            if (reduceL2 > 0 && reduceL3 <= 0) {
                return true;
            } else {
                return reduceL2 <= 0 && reduceL3 > 0;
            }
        } else {
            if (reduceL2 <= 0) {
                return false;
            } else {
                return reduceL3 > 0;
            }
        }
    }

    /**
     * Returns the phase that is under the Phase Limit.
     * NOTE: Only call after check has been done that only one is under the limit in the first place.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @return Index of the Phase that is under the Limit
     */
    private int getOnePhaseUnderPhaseLimit(int reduceL1, int reduceL2) {
        if (reduceL1 <= 0) {
            return 1;
        }
        if (reduceL2 <= 0) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * Checks if all Phases are over the Phase Limit.
     *
     * @param reduceL1 The Amount that has to be reduced from L1
     * @param reduceL2 The Amount that has to be reduced from L2
     * @param reduceL3 The Amount that has to be reduced from L3
     * @return true if only one is over the limit
     */
    private boolean threePhasesOverPhaseLimit(int reduceL1, int reduceL2, int reduceL3) {
        return (reduceL1 > 0 && reduceL2 > 0 && reduceL3 > 0);
    }

    //----------------Methods for Load Balancing-----------------\\

    /**
     * Updates the current Power consumption and returns an Array of problematic EVCS Arrays if an unbalanced load exists.
     *
     * @return Array of ManagedEvcs[]
     */
    private Optional<List<ManagedEvcs[]>> getRequestedPower() {
        this.updatePower(false);
        //If the load should be checked this will calculate if there is a load Delta >= the maximum allowed Delta
        int min = this.getMinimumLoad();
        this.getMiddleLoad();
        int max = this.getMaximumLoad();
        if (max - min > this.config.symmetryDelta()) {
            if (this.config.symmetry()) {
                return this.unbalancedEvcsOnPhase();
            }
        }
        return Optional.empty();
    }

    /**
     * Balances the Power if symmetry was enabled.
     * Checks how much power needs to be reduces so the delta between the phases is less than the maximumLoadDelta.
     *
     * @param problem A list of all EVCS that have to be limited
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private void limitPower(List<ManagedEvcs[]> problem) throws Exception {
        //TODO this should be rewritten too
        int powerDelta = this.max - this.min;
        int powerDelta2 = this.middle - this.min;
        //The Power that have to be reduced to create balance
        int amountLeft = powerDelta - this.config.symmetryDelta() + 1;
        int amountLeft2 = powerDelta2 - this.config.symmetryDelta() + 1;
        ManagedEvcs[] onePhase = problem.get(ONE_PHASE_INDEX);
        ManagedEvcs[] onePhase2;
        int onePhaseLength2;
        int onePhaseLength = onePhase.length;
        int amountToReduceOnePhase = 0;
        if (onePhaseLength != 0) {
            amountToReduceOnePhase = amountLeft / onePhaseLength;
        }
        ManagedEvcs[] twoPhase = problem.get(TWO_PHASE_INDEX);
        int twoPhaseLength = twoPhase.length;
        ManagedEvcs[] twoPhase2;
        int twoPhaseLength2 = 0;
        if (amountToReduceOnePhase > 0) {
            amountLeft = this.reduceOnePhaseEvcs(onePhase, amountToReduceOnePhase, amountLeft);
        }
        if (amountLeft <= 0) {
            this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
        } else {
            int amountToReduceTwoPhase = 0;
            if (twoPhaseLength != 0) {
                amountToReduceTwoPhase = amountLeft / twoPhaseLength;
            }
            int[] amountsLeft = new int[2];
            amountsLeft[0] = amountLeft;
            amountsLeft[1] = amountLeft2;
            if (amountToReduceTwoPhase > 0) {
                this.reduceTwoPhaseEvcs(twoPhase, amountToReduceTwoPhase, amountsLeft);
            }
            amountLeft = amountsLeft[0];
            amountLeft2 = amountsLeft[1];
            if (amountLeft <= 0) {
                this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");

            } else {
                //If after reducing the one and two phase EVCS was not enough,
                // this will reduce the one phase EVCS until its impossible to do it anymore
                int previousAmountLeft = amountLeft;
                while (amountLeft > 0) {
                    if (onePhaseLength != 0) {
                        amountToReduceOnePhase = amountLeft / onePhaseLength;

                        amountLeft = this.reduceOnePhaseEvcs(onePhase, amountToReduceOnePhase, amountLeft);
                    }
                    if (amountLeft != previousAmountLeft) {
                        previousAmountLeft = amountLeft;
                    } else {
                        break;
                    }
                }
                if (amountLeft <= 0) {
                    this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
                }

            }
        }
        //If two phases are unbalanced
        if (problem.size() > 2) {
            onePhase2 = problem.get(ONE_PHASE_INDEX_2);
            onePhaseLength2 = onePhase2.length;
            twoPhase2 = problem.get(TWO_PHASE_INDEX_2);
            twoPhaseLength2 = twoPhase2.length;
            if (amountLeft2 > 0) {
                int amountToReduce2 = 0;
                if (onePhaseLength2 != 0) {
                    amountToReduce2 = amountLeft2 / onePhaseLength2;
                    amountLeft2 = this.reduceOnePhaseEvcs(onePhase2, amountToReduce2, amountLeft2);
                }
            }
            if (amountLeft2 <= 0) {
                this.log.info("Phase " + this.middleIndex + " has been successfully Balanced.");

            } else {
                //If after reducing the one and two phase EVCS was not enough,
                // this will reduce the one phase EVCS until its impossible to do it anymore
                int previousAmountLeft2 = amountLeft2;
                while (amountLeft2 > 0) {
                    int amountToReduce2 = 1;
                    if (onePhaseLength2 != 0) {
                        amountToReduce2 = amountLeft2 / onePhaseLength2;
                        amountLeft2 = this.reduceOnePhaseEvcs(onePhase2, amountToReduce2, amountLeft2);
                    }
                    if (amountLeft2 != previousAmountLeft2) {
                        previousAmountLeft2 = amountLeft;
                    } else {
                        break;
                    }
                }
                if (amountLeft2 <= 0) {
                    this.log.info("Phase " + this.maxIndex + " has been successfully Balanced.");
                }
            }
        }
        //If it's still unbalanced this will reduce the two phase evcs until its not possible anymore
        if (amountLeft > 0 || (amountLeft2 > 0 && problem.size() > 2)) {
            int[] amountsLeft = new int[2];
            amountsLeft[0] = amountLeft;
            amountsLeft[1] = amountLeft2;
            int[] previousAmountsLeft = amountsLeft.clone();
            while (amountLeft > 0 || (amountLeft2 > 0 && problem.size() > 2)) {
                int amountToReduceTwoPhase = 0;
                if (amountLeft > 0 && twoPhaseLength != 0) {
                    amountToReduceTwoPhase = amountLeft / twoPhaseLength;
                } else if (twoPhaseLength2 != 0) {
                    amountToReduceTwoPhase = amountLeft2 / twoPhaseLength2;
                }
                if (amountLeft == 1 || amountToReduceTwoPhase == 0) {
                    amountToReduceTwoPhase = 1;
                }
                if (amountToReduceTwoPhase != 0) {
                    this.reduceTwoPhaseEvcs(twoPhase, amountToReduceTwoPhase, amountsLeft);
                    amountLeft = amountsLeft[0];
                    amountLeft2 = amountsLeft[1];
                }
                if (amountLeft != previousAmountsLeft[0] || amountLeft2 != previousAmountsLeft[1]) {
                    previousAmountsLeft = amountsLeft.clone();
                } else {
                    this.log.error("Phases can not be balanced!");
                    throw new Exception();
                }
            }
        }
        this.log.info("Balance has been successfully restored.");

    }

    //-------------------Power Off Methods--------------------\\

    //-----------Power Off for Balancing--------\\

    /**
     * Turns off EVCS for the Phase Balancing.
     * Priority List:
     * 1. Two Phase EVCS that charge on two unbalanced Phases
     * 2. One Phase EVCS
     * 3. Two Phase EVCS
     */
    private void turnOffEvcsBalance() throws Exception {
        this.updatePower(true);

        int max = this.getMaximumLoad();
        int middle = this.getMiddleLoad();
        int min = this.getMinimumLoad();

        //Determining on what phase/-s are the problem/-s
        Optional<List<ManagedEvcs[]>> optionalProblem = this.unbalancedEvcsOnPhase();
        if (optionalProblem.isPresent()) {
            List<ManagedEvcs[]> problem = optionalProblem.get();
            ManagedEvcs[] onePhase = problem.get(ONE_PHASE_INDEX);
            ManagedEvcs[] twoPhase = problem.get(TWO_PHASE_INDEX);
            ManagedEvcs[] onePhase2 = null;
            ManagedEvcs[] twoPhase2 = null;
            if (problem.size() > 2) {
                onePhase2 = problem.get(ONE_PHASE_INDEX_2);
                twoPhase2 = problem.get(TWO_PHASE_INDEX_2);
            }

            //--------Handle all Two phase EVCS that charge with both problem Phases--------\\
            //If two of the Phases are unbalanced, then the middle Phase has to be lowered here.
            //Since it is less likely that there are twophase Evcs on both of these phases, they are limited first.
            if (middle - min > this.config.symmetryDelta() && twoPhase.length > 0) {
                if (!this.turnOffTwoPhaseDoubleHitEvcsBalancing(min, twoPhase)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            this.getMiddleLoad();
            min = this.getMinimumLoad();
            twoPhase = this.removeEvcsFromArray(twoPhase);
            if (twoPhase2 != null) {
                twoPhase2 = this.removeEvcsFromArray(twoPhase2);
            }

            //----------------Handle the One phase EVCS on one Phase------------------\\
            if (max - min > this.config.symmetryDelta() && onePhase.length > 0) {
                if (!this.turnOffOnePhaseEvcsBalancing(min, onePhase, false)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            this.removeEvcsFromArray(onePhase);

            //-------Handle the One phase EVCS on the other Phase (if necessary)--------\\
            if (middle - min > this.config.symmetryDelta() && onePhase2 != null) {
                if (!this.turnOffOnePhaseEvcsBalancing(min, onePhase2, true)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            this.getMiddleLoad();
            min = this.getMinimumLoad();
            if (onePhase2 != null) {
                this.removeEvcsFromArray(onePhase2);
            }

            //------Handle the Two Phase EVCS on the Max Phase (if necessary)--------\\
            if (max - min > this.config.symmetryDelta() && twoPhase.length > 0) {
                if (!this.turnOffTwoPhaseEvcsBalancing(min, twoPhase)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            this.getMaximumLoad();
            middle = this.getMiddleLoad();
            min = this.getMinimumLoad();
            twoPhase = this.removeEvcsFromArray(twoPhase);


            //------Handle the Two Phase EVCS on the Middle Phase (if necessary)--------\\
            if (middle - min > this.config.symmetryDelta() && twoPhase2 != null) {
                if (!this.turnOffTwoPhaseEvcsBalancing(min, twoPhase2)) {
                    this.log.info("Successfully Balanced Phases by turning off EVCS/s");
                    return;
                }
            }
            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            this.getMiddleLoad();
            min = this.getMinimumLoad();
            this.removeEvcsFromArray(twoPhase);

            if (max - min > this.config.symmetryDelta()) {
                throw new Exception("Its impossible to balance the Phases. This should not have happened.");
            }

        } else {
            this.log.info("Phases already Balanced.");
        }
    }

    /**
     * Turns off one phase EVCS that are the Unbalanced Phase until its either balanced or no EVCS are left.
     *
     * @param minimum  Lowest Power Consumption
     * @param onePhase All one phase EVCS that charge on the appropriate Phase
     * @param phase2   true if this is not on the Max Phase but on Middle
     * @return true if the Phases are now balanced
     */
    private boolean turnOffOnePhaseEvcsBalancing(int minimum, ManagedEvcs[] onePhase, boolean phase2) {
        boolean unbalanced = true;
        int onePhaseLength = onePhase.length;
        if (onePhaseLength > 0) {
            int i = 0;

            while (unbalanced && onePhaseLength > 0) {
                if (onePhase[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                    i++;
                    continue;
                }
                try {
                    this.turnOffEvcs(onePhase[i]);
                    int minPower = this.minPower(onePhase[i]);
                    this.powerWaitingList.put(onePhase[i].id(), //
                            new EvcsOnHold(minPower, Instant.now(), 1, true));
                    this.updatePower(true);
                    int maximum;
                    if (phase2) {
                        maximum = this.getMiddleLoad();
                    } else {
                        maximum = this.getMaximumLoad();
                    }
                    onePhaseLength--;
                    i++;

                    if (maximum - minimum > this.config.symmetryDelta()) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " One Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }


    /**
     * Turns off Two phase EVCS that are on both Unbalanced Phases until its either balanced or no EVCS are left.
     *
     * @param min      Lowest Power Consumption
     * @param twoPhase All two phase EVCS that charge on the appropriate Phases
     * @return true if the Phases are now balanced
     */
    private boolean turnOffTwoPhaseEvcsBalancing(int min, ManagedEvcs[] twoPhase) {
        boolean unbalanced = true;
        int twoPhaseLength = twoPhase.length;
        if (twoPhaseLength > 0) {
            int i = 0;
            while (unbalanced && twoPhaseLength > 0) {
                if (twoPhase[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                    i++;
                    continue;
                }
                try {
                    this.turnOffEvcs(twoPhase[i]);
                    int minPower = this.minPower(twoPhase[i]);
                    this.powerWaitingList.put(twoPhase[i].id(),//
                            new EvcsOnHold(minPower, Instant.now(), 2, true));
                    this.updatePower(true);
                    int maximum = this.getMaximumLoad();
                    int middleLoad = this.getMiddleLoad();
                    twoPhaseLength--;
                    i++;
                    if (this.balance(maximum, middleLoad, min)) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }

    /**
     * Turns off Two phase EVCS that are on both Unbalanced Phases until its either balanced or no EVCS are left.
     *
     * @param min      Lowest Power Consumption
     * @param twoPhase All two phase EVCS that charge on the appropriate Phases
     * @return true if the Phases are now balanced
     */
    private boolean turnOffTwoPhaseDoubleHitEvcsBalancing(int min, ManagedEvcs[] twoPhase) {
        boolean unbalanced = true;
        ManagedEvcs[] twoPhaseDoubleHit = this.getTwoPhaseEvcs(twoPhase, this.maxIndex, this.minIndex);
        int twoPhaseDoubleHitLength = twoPhaseDoubleHit.length;
        if (twoPhaseDoubleHitLength > 0) {
            int i = 0;
            while (unbalanced && twoPhaseDoubleHitLength > 0) {
                if (twoPhaseDoubleHit[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                    i++;
                    continue;
                }
                try {
                    this.turnOffEvcs(twoPhaseDoubleHit[i]);
                    int minPower = this.minPower(twoPhaseDoubleHit[i]);
                    this.powerWaitingList.put(twoPhaseDoubleHit[i].id(),//
                            new EvcsOnHold(minPower, Instant.now(), 2, true));
                    this.updatePower(true);
                    int maximum = this.getMaximumLoad();
                    int middleLoad = this.getMiddleLoad();
                    twoPhaseDoubleHitLength--;
                    i++;
                    if (this.balance(maximum, middleLoad, min)) {
                        unbalanced = false;

                    }

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return unbalanced;
    }
    //--------------Power Off for Phase Limitation--------------\\

    /**
     * Turns off EVCS for the Phase Limit.
     * Priority List:
     * 1. All Three Phases are over the Limit:
     * 1.1 Three Phasers
     * 1.2. Two Phasers IF Three Phasers don't exist / not enough to reduce at least one Phase
     * 1.3 One Phasers IF neither Three nor Two Phasers exist / not enough to reduce at least one Phase
     * 2. Two phases are over the Limit:
     * 2.1 Two Phasers on both Phases over the Limit
     * 2.2 One Phasers IF no Two Phasers of the above condition exist / not enough to reduce at least one Phase
     * 2.3 Three Phasers IF neither Three nor Two Phasers of above condition exist / not enough to reduce at least one Phase
     * 2.4 Two Phasers IF none of the above apply
     * 3. One Phases is over the Limit:
     * 3.1 One Phasers
     * 3.2 Three Phasers IF no One Phasers exist / not enough to reduce at least one Phase
     * 3.3 Two Phasers IF neither Three Phasers nor One Phasers exist / not enough to reduce at least one Phase
     */
    private void turnOffEvcsPhaseLimit() throws Exception {
        this.updatePower(true);
        int powerToReduceL1 = this.currentL1 - (this.phaseLimit / GRID_VOLTAGE);
        int powerToReduceL2 = this.currentL2 - (this.phaseLimit / GRID_VOLTAGE);
        int powerToReduceL3 = this.currentL3 - (this.phaseLimit / GRID_VOLTAGE);
        ManagedEvcs[] threePhases = this.getThreePhaseEvcs();
        int minReduce = Math.min(Math.min(powerToReduceL1, powerToReduceL2), powerToReduceL3);
        int minIndex = this.getPhaseByPower(powerToReduceL1, powerToReduceL2, powerToReduceL3, minReduce);
        int phaseOkIndex;

        //--------1. All Three Phases are over the Limit-------\\
        if (this.threePhasesOverPhaseLimit(powerToReduceL1, powerToReduceL2, powerToReduceL3)) {
            //--------1.1 Reduce Three Phasers if they exist----------\\
            if (threePhases.length > 0) {
                int reduceDelta;
                minReduce = this.turnOffThreePhaseEvcs(threePhases, minReduce);
                switch (minIndex) {
                    case 1:
                        reduceDelta = powerToReduceL1 - minReduce;
                        break;
                    case 2:
                        reduceDelta = powerToReduceL2 - minReduce;
                        break;
                    case 3:
                        reduceDelta = powerToReduceL3 - minReduce;
                        break;
                    default:
                        reduceDelta = 0;
                }
                powerToReduceL1 -= reduceDelta;
                powerToReduceL2 -= reduceDelta;
                powerToReduceL3 -= reduceDelta;
            }
            //--------If there are no Three Phasers or not enough----------\\
            if (minReduce > 0) {
                ManagedEvcs[] twoPhases = this.getTwoPhaseEvcs(minIndex);

                //-------1.2 Reduce Two Phasers if they exist--------\\
                if (twoPhases.length > 0) {

                    ManagedEvcs[] twoPhases1 = this.getTwoPhaseEvcs(twoPhases, this.maxIndex, this.middleIndex);
                    ManagedEvcs[] twoPhases2 = this.getTwoPhaseEvcs(twoPhases, this.middleIndex, this.maxIndex);
                    if (twoPhases1.length > 0 || twoPhases2.length > 0) {
                        int reduceByGroup1;
                        int reduceByGroup2;
                        int reduce1Delta = 0;
                        int reduce2Delta = 0;

                        if (twoPhases1.length > 0 && twoPhases2.length > 0) {
                            reduceByGroup1 = Math.floorDiv(minReduce, 2);
                            reduceByGroup2 = Math.floorDiv(minReduce, 2) + 1;
                            reduceByGroup1 = this.turnOffTwoPhaseEvcs(twoPhases1, reduceByGroup1);
                            reduce1Delta = Math.floorDiv(minReduce, 2) - reduceByGroup1;
                            reduceByGroup2 = this.turnOffTwoPhaseEvcs(twoPhases2, reduceByGroup2);
                            reduce2Delta = (Math.floorDiv(minReduce, 2) + 1) - reduceByGroup2;
                        } else if (twoPhases1.length > 0) {
                            reduceByGroup1 = this.turnOffTwoPhaseEvcs(twoPhases1, minReduce);
                            reduce1Delta = minReduce - reduceByGroup1;
                        } else {
                            reduceByGroup2 = this.turnOffTwoPhaseEvcs(twoPhases2, minReduce);
                            reduce2Delta = minReduce - reduceByGroup2;
                        }
                        minReduce -= (reduce1Delta + reduce2Delta);
                        powerToReduceL1 -= this.allocateReduceToPhase(//
                                1, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                        powerToReduceL2 -= this.allocateReduceToPhase(//
                                2, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                        powerToReduceL3 -= this.allocateReduceToPhase(//
                                3, twoPhases1, twoPhases2, reduce1Delta, reduce2Delta);
                    }
                }
                //-----1.3 Reduce One Phasers. If this code is reached and they don't exist, something went wrong-----\\
                if (minReduce > 0) {
                    ManagedEvcs[] onePhases = this.getOnePhaseEvcs(minIndex);
                    minReduce = this.turnOffOnePhaseEvcs(onePhases, minReduce);
                    switch (minIndex) {
                        case 1:
                            powerToReduceL1 -= minReduce;
                            break;
                        case 2:
                            powerToReduceL2 -= minReduce;
                            break;
                        case 3:
                            powerToReduceL3 -= minReduce;
                            break;
                    }
                }
                if (minReduce > 0) {
                    throw new Exception();
                }
            }
        }
        phaseOkIndex = minIndex;

        //------------------2.Reduce the Second Phase--------------\\
        if (this.twoPhasesOverPhaseLimit(powerToReduceL1, powerToReduceL2, powerToReduceL3)) {
            minReduce = this.getMiddleReduce(powerToReduceL1, powerToReduceL2, powerToReduceL3);
            minIndex = this.getPhaseByPower(powerToReduceL1, powerToReduceL2, powerToReduceL3, minReduce);

            //---------------2.1 Reduce Two Phasers on both Phases over the Limit--------------\\
            ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(minIndex, phaseOkIndex);
            if (twoPhase.length > 0) {
                int reducedByTwoPhase = this.turnOffTwoPhaseEvcs(twoPhase, minReduce);
                int reduceDelta = minReduce - reducedByTwoPhase;
                switch (phaseOkIndex) {
                    case 1:
                        powerToReduceL2 -= reduceDelta / 2;
                        powerToReduceL3 -= reduceDelta / 2;
                        break;
                    case 2:
                        powerToReduceL1 -= reduceDelta / 2;
                        powerToReduceL3 -= reduceDelta / 2;
                        break;
                    case 3:
                        powerToReduceL1 -= reduceDelta / 2;
                        powerToReduceL2 -= reduceDelta / 2;
                        break;
                }
                minReduce = reducedByTwoPhase;

            }
            //--------------2.2 One Phasers--------------\\
            if (minReduce > 0) {

                ManagedEvcs[] onePhases = this.getOnePhaseEvcs(minIndex);
                if (onePhases.length > 0) {
                    minReduce = this.turnOffOnePhaseEvcs(onePhases, minReduce);
                    switch (minIndex) {
                        case 1:
                            powerToReduceL1 -= minReduce;
                            break;
                        case 2:
                            powerToReduceL2 -= minReduce;
                            break;
                        case 3:
                            powerToReduceL3 -= minReduce;
                            break;

                    }
                }
            }
            //--------------2.3 Three Phasers--------------\\
            if (minReduce > 0) {
                threePhases = this.removeEvcsFromArray(threePhases);
                if (threePhases.length > 0) {
                    int reduceDelta;
                    minReduce = this.turnOffThreePhaseEvcs(threePhases, minReduce);
                    reduceDelta = powerToReduceL1 - minReduce;
                    powerToReduceL1 -= reduceDelta;
                    powerToReduceL2 -= reduceDelta;
                    powerToReduceL3 -= reduceDelta;
                }
            }
            //-----2.4 Two Phasers if none of the above worked. If this code is reached and they don't exist something went wrong.-----\\
            if (minReduce > 0) {
                twoPhase = this.getTwoPhaseEvcs(minIndex);
                if (twoPhase.length > 0) {
                    int reducedByTwoPhase = this.turnOffTwoPhaseEvcs(twoPhase, minReduce);
                    int reduceDelta = minReduce - reducedByTwoPhase;
                    switch (minIndex) {
                        case 1:
                            powerToReduceL1 -= reduceDelta / 2;
                            break;
                        case 2:
                            powerToReduceL2 -= reduceDelta / 2;
                            break;
                        case 3:
                            powerToReduceL3 -= reduceDelta / 2;
                            break;
                    }
                    switch (phaseOkIndex) {
                        case 1:
                            powerToReduceL1 -= reduceDelta / 2;
                            break;
                        case 2:
                            powerToReduceL2 -= reduceDelta / 2;
                            break;
                        case 3:
                            powerToReduceL3 -= reduceDelta / 2;
                            break;
                    }
                    minReduce = reducedByTwoPhase;
                }
            }
            if (minReduce > 0) {
                throw new Exception();
            }
        }

        //----------------------3. Reduce Last Phase---------------------\\
        minIndex = this.getTwoPhasesUnderPhaseLimit(powerToReduceL1, powerToReduceL2, powerToReduceL3);
        switch (minIndex) {
            case 1:
                minReduce = powerToReduceL1;
                break;
            case 2:
                minReduce = powerToReduceL2;
                break;
            case 3:
                minReduce = powerToReduceL3;
                break;
        }
        if (minReduce > 0) {
            //----------------3.1. One Phasers------------------\\
            ManagedEvcs[] onePhase = this.getOnePhaseEvcs(minIndex);
            if (onePhase.length > 0) {
                minReduce = this.turnOffOnePhaseEvcs(onePhase, minReduce);
                switch (minIndex) {
                    case 1:
                        powerToReduceL1 -= minReduce;
                        break;
                    case 2:
                        powerToReduceL2 -= minReduce;
                        break;
                    case 3:
                        powerToReduceL3 -= minReduce;
                        break;

                }
            }

            //---------------3.2 Three Phasers---------------\\
            if (minReduce > 0) {
                threePhases = this.removeEvcsFromArray(threePhases);
                if (threePhases.length > 0) {
                    int reduceDelta;
                    minReduce = this.turnOffThreePhaseEvcs(threePhases, minReduce);
                    reduceDelta = powerToReduceL1 - minReduce;
                    powerToReduceL1 -= reduceDelta;
                    powerToReduceL2 -= reduceDelta;
                    powerToReduceL3 -= reduceDelta;
                }
            }
            //-------------3.3 Two Phasers----------------\\
            if (minReduce > 0) {
                ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(minIndex);
                if (twoPhase.length > 0) {
                    minReduce = this.turnOffTwoPhaseEvcs(twoPhase, minReduce);

                }
            }
            //Every available option was tested to Limit the phases.
            //If somehow this failed it will throw an Exception.
            if (minReduce > 0) {
                throw new Exception();
            }

            //--------Check Balance--------\\
            int max = this.getMaximumLoad();
            int middle = this.getMiddleLoad();
            int min = this.getMinimumLoad();
            if (!this.balance(max, middle, min)) {
                this.turnOffEvcsBalance();
            }
        }

    }

    /**
     * Returns the second highest Reduce value.
     *
     * @param powerToReduceL1 Reduce value of L1
     * @param powerToReduceL2 Reduce value of L2
     * @param powerToReduceL3 Reduce value of L3
     * @return the second highest Reduce value
     */
    private int getMiddleReduce(int powerToReduceL1, int powerToReduceL2, int powerToReduceL3) {
        if (powerToReduceL1 > powerToReduceL2 && powerToReduceL2 > powerToReduceL3) {
            return powerToReduceL2;
        } else if (powerToReduceL1 > powerToReduceL2 && powerToReduceL3 > powerToReduceL2) {
            return powerToReduceL3;
        } else if (powerToReduceL3 > powerToReduceL2 && powerToReduceL2 > powerToReduceL1) {
            return powerToReduceL2;
        } else if (powerToReduceL3 > powerToReduceL2 && powerToReduceL1 > powerToReduceL2) {
            return powerToReduceL1;
        } else {
            return Math.max(powerToReduceL1, powerToReduceL3);
        }
    }


    /**
     * Allocated Reduce Amounts to the Phase it belongs to.
     * NOTE: only for the Context where Two Phase EVCS have been turned off for the Phase Limit.
     *
     * @param phaseNumber  The Phase that has to be reduced
     * @param twoPhases1   the first Group of Two Phase Evcs
     * @param twoPhases2   the second Group of Two Phase Evcs
     * @param reduce1Delta the power Reduced by Group 1
     * @param reduce2Delta the power Reduced by Group 2
     * @return the appropriate reduce amount
     */
    private int allocateReduceToPhase(int phaseNumber, ManagedEvcs[] twoPhases1, ManagedEvcs[] twoPhases2,
                                      int reduce1Delta, int reduce2Delta) {
        ManagedEvcs tp1 = twoPhases1[0];
        int[] tpPhases = tp1.getPhaseConfiguration();
        if (tpPhases[0] == phaseNumber || tpPhases[1] == phaseNumber) {
            return reduce1Delta;
        }
        ManagedEvcs tp2 = twoPhases2[0];
        int[] tp2Phases = tp2.getPhaseConfiguration();
        if (tp2Phases[0] == phaseNumber || tp2Phases[1] == phaseNumber) {
            return reduce2Delta;
        }

        return reduce1Delta + reduce2Delta;
    }

    //--------------Power Off for Power Limitation--------------\\

    /**
     * Turns off EVCS for the Power Limit
     * Priority List:
     * 1. Three Phases
     * 2. Two Phases on the two highest Phases (if existent)
     * 3. Two Phases
     * 4. One Phase
     */
    private void turnOffEvcsPowerLimit() throws Exception {
        this.updatePower(true);
        int powerToReduce = this.currentL1 + this.currentL2 + this.currentL3 - this.powerLimit / GRID_VOLTAGE;
        if (powerToReduce > 0) {
            int max;
            int middle;
            int min;
            ManagedEvcs[] onePhases;
            ManagedEvcs[] twoPhases = this.getTwoPhaseEvcs();
            ManagedEvcs[] threePhases = this.getThreePhaseEvcs();

            //-----Power Off Three Phase EVCS because they won't create balance issues-----\\
            if (threePhases.length > 0) {
                powerToReduce = this.turnOffThreePhaseEvcs(threePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    return;
                }
            }

            //------------------------------Power Off Two Phase EVCS---------------------------------\\
            if (twoPhases.length > 0) {
                powerToReduce = this.turnOffTwoPhaseEvcs(twoPhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

            //--------Update internal Values--------\\
            max = this.getMaximumLoad();
            this.getMiddleLoad();
            this.getMinimumLoad();
            onePhases = this.getOnePhaseEvcs(this.getPhaseByPower(max));
            //-----------------------------Power Off One Phase EVCS----------------------------------\\
            if (onePhases.length > 0) {
                powerToReduce = this.turnOffOnePhaseEvcs(onePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

            //--------Update internal Values--------\\
            this.getMaximumLoad();
            middle = this.getMiddleLoad();
            this.getMinimumLoad();
            onePhases = this.getOnePhaseEvcs(this.getPhaseByPower(middle));

            //-----------------------------Power Off One Phase EVCS----------------------------------\\
            if (onePhases.length > 0) {
                powerToReduce = this.turnOffOnePhaseEvcs(onePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }
            //--------Update internal Values--------\\
            this.getMaximumLoad();
            this.getMiddleLoad();
            min = this.getMinimumLoad();
            onePhases = this.getOnePhaseEvcs(this.getPhaseByPower(min));

            //-----------------------------Power Off One Phase EVCS----------------------------------\\
            if (onePhases.length > 0) {
                powerToReduce = this.turnOffOnePhaseEvcs(onePhases, powerToReduce);
                if (powerToReduce <= 0) {
                    this.log.info("Power is under the Limit.");
                    if (!this.balance(this.getMaximumLoad(), this.getMiddleLoad(), this.getMinimumLoad())) {
                        this.turnOffEvcsBalance();
                    }
                    return;
                }
            }

        } else {
            this.log.info("Already under the Power Limit. This should not have happened.");
        }
    }


    //-----------------------General Turn Offs-------------------------\\

    /**
     * Turns off One phase EVCS until its either under the Power Limit or no EVCS are left.
     *
     * @param onePhases     All one phase EVCS that charge on the appropriate Phase
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffOnePhaseEvcs(ManagedEvcs[] onePhases, int powerToReduce) {
        int powerRemoved;
        int onePhaseLength = onePhases.length;
        if (onePhaseLength > 0) {
            int i = 0;
            while (powerToReduce > 0 && onePhaseLength > 0) {
                if (onePhases[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                    i++;
                    continue;
                }
                try {
                    powerRemoved = this.turnOffEvcs(onePhases[i]);
                    int minPower = this.minPower(onePhases[i]);
                    this.powerWaitingList.put(onePhases[i].id(),//
                            new EvcsOnHold(minPower, Instant.now(), 1, true));
                    this.updatePower(true);
                    powerToReduce -= powerRemoved;
                    onePhaseLength--;
                    i++;

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " One Phase EVCS/s have been turned off.");
        }
        return powerToReduce;
    }


    /**
     * Turns off Two phase EVCS that are on the highest Phases until its either under the Power Limit or no EVCS are left.
     *
     * @param twoPhase      All two phase EVCS that charge on the appropriate Phases
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffTwoPhaseEvcs(ManagedEvcs[] twoPhase, int powerToReduce) {
        int powerRemoved;
        ManagedEvcs[] twoPhaseDoubleHit = this.getTwoPhaseEvcs(twoPhase, this.maxIndex, this.minIndex);
        int twoPhaseDoubleHitLength = twoPhaseDoubleHit.length;
        if (twoPhaseDoubleHitLength > 0) {
            int i = 0;
            while (powerToReduce > 0 && twoPhaseDoubleHitLength > 0) {
                if (twoPhaseDoubleHit[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                    i++;
                    continue;
                }
                try {
                    powerRemoved = this.turnOffEvcs(twoPhaseDoubleHit[i]);
                    int minPower = this.minPower(twoPhaseDoubleHit[i]);
                    this.powerWaitingList.put(twoPhaseDoubleHit[i].id(),//
                            new EvcsOnHold(minPower, Instant.now(), 2, true));
                    this.updatePower(true);
                    powerToReduce -= powerRemoved;
                    twoPhaseDoubleHitLength--;
                    i++;

                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Unable to turn Off EVCS. This should not have happened.");
                }
            }
            this.log.info(i + " Two Phase EVCS/s have been turned off.");
        }
        return powerToReduce;
    }

    /**
     * Turns off Three Phase EVCS until its either under the Power Limit or no EVCS are left.
     *
     * @param threePhases   All three phase EVCS
     * @param powerToReduce Power that has to be reduced to be under the Power Limit
     * @return powerToReduce that is left
     */
    private int turnOffThreePhaseEvcs(ManagedEvcs[] threePhases, int powerToReduce) {
        int powerRemoved;
        int threePhaseLength = threePhases.length;
        int i = 0;
        while (powerToReduce > 0 && threePhaseLength > 0) {
            if (threePhases[i].getIsPriority().get() && this.nonPriorityAmount > 0) {
                i++;
                continue;
            }
            try {
                powerRemoved = this.turnOffEvcs(threePhases[i]);
                int minPower = this.minPower(threePhases[i]);
                this.powerWaitingList.put(threePhases[i].id(),//
                        new EvcsOnHold(minPower, Instant.now(), 3, true));
                this.updatePower(true);
                powerToReduce -= powerRemoved;
                threePhaseLength--;
                i++;
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Unable to turn Off EVCS. This should not have happened.");
            }
        }
        this.log.info(i + " Three Phase EVCS/s have been turned off.");

        return powerToReduce;
    }


    /**
     * Turns an EVCS off and returns their old Power Value.
     *
     * @param evcs The EVCS that has to be turned off
     * @return Last Power Value
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int turnOffEvcs(ManagedEvcs evcs) throws OpenemsError.OpenemsNamedException {
        int oldPower;
        if (evcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                evcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
            oldPower = ((evcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    evcs.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE));
        } else {
            oldPower = (evcs.getChargePower().get() / GRID_VOLTAGE);
        }

        if (evcs.getStatus() == Status.CHARGING) {
            evcs.setChargePowerLimit(OFF);
        } else {
            evcs.setChargePowerLimit(Math.max(evcs.getMinimumHardwarePower().orElse(//
                    MINIMUM_POWER), evcs.getMinimumPower().orElse(MINIMUM_POWER)) / GRID_VOLTAGE);
        }
        this.removeEvcsFromActive(evcs);
        return oldPower;
    }


    //--------------------General Methods for Limiting-------------------\\

    /**
     * Reduces the Power of EVCS that charge with only one Phase by a amount given.
     *
     * @param onePhase       Array of all EVCS that have to be reduced
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @param amountLeft     The Sum of what has to be reduced by all EVCS
     * @return modified amountLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceOnePhaseEvcs(ManagedEvcs[] onePhase, int amountToReduce, int amountLeft) throws
            OpenemsError.OpenemsNamedException {
        for (ManagedEvcs currentEvcs : onePhase) {
            if (currentEvcs.getIsPriority().get() && this.nonPriorityAmount > 0//
                    && (!this.config.symmetry() && this.max - this.min < this.config.symmetryDelta())) {
                continue;
            }
            int newPower;
            if (amountLeft == 1 && amountToReduce == 0) {
                amountToReduce = 1;
            }
            if (currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    currentEvcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
                newPower = ((currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        currentEvcs.getSetChargePowerLimitChannel().value().orElse(0))//
                        / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (currentEvcs.getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
            int minPower = this.minPower(currentEvcs);
            if (newPower >= minPower) {
                currentEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountLeft -= amountToReduce;
                this.log.info(currentEvcs.id() + " was reduced by " + amountToReduce * GRID_VOLTAGE//
                        + " W and is now at " + newPower * GRID_VOLTAGE + " W");
            }
        }
        return amountLeft;
    }

    /**
     * Reduces the Power of EVCS that charge with two Phases by a amount given.
     * NOTE: This Method doesn't check if its allowed to do it. This Should only be used in the Phase Limitation.
     *
     * @param twoPhase       Array of all EVCS that have to be reduced
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @return How much was reduced
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceTwoPhaseEvcs(ManagedEvcs[] twoPhase, int amountToReduce) throws
            OpenemsError.OpenemsNamedException {
        int amountReduced = 0;
        if (amountToReduce == 0) {
            amountToReduce = 1;
        }
        for (ManagedEvcs currentEvcs : twoPhase) {
            if (currentEvcs.getIsPriority().get() && this.nonPriorityAmount > 0//
                    && (!this.config.symmetry() && this.max - this.min < this.config.symmetryDelta())) {
                continue;
            }
            int newPower;
            if (currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    currentEvcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
                newPower = ((currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        currentEvcs.getSetChargePowerLimitChannel().value().orElse(0))//
                        / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (currentEvcs.getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
            int minPower = this.minPower(currentEvcs);
            if (newPower >= minPower) {
                currentEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountReduced += amountToReduce;
            }
        }
        return amountReduced;
    }

    /**
     * Reduces the Power of EVCS that charge with two Phases by a amount given.
     *
     * @param twoPhase       Array of all EVCS that have to be reduced
     * @param amountToReduce Amount that has to be reduced per EVCS
     * @param amountsLeft    A tuple of the Sums of what has to be reduced by all EVCS
     * @return modified amountsLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int[] reduceTwoPhaseEvcs(ManagedEvcs[] twoPhase, int amountToReduce,
                                     int[] amountsLeft) throws OpenemsError.OpenemsNamedException {
        for (ManagedEvcs currentEvcs : twoPhase) {
            if (currentEvcs.getIsPriority().get() && this.nonPriorityAmount > 0//
                    && (!this.config.symmetry() && this.max - this.min < this.config.symmetryDelta())) {
                continue;
            }
            // In case there is a remainder after the division
            if ((amountsLeft[0] == 1 || amountsLeft[1] == 1) && amountToReduce == 0) {
                amountToReduce = 1;
            }
            int[] phaseConfiguration = currentEvcs.getPhaseConfiguration();
            if (this.min2Index == 0 && (phaseConfiguration[0] != this.minIndex && phaseConfiguration[1] != this.minIndex)) {
                int newPower;
                if (currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        currentEvcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
                    newPower = ((currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                            currentEvcs.getSetChargePowerLimitChannel().value().orElse(0))//
                            / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (currentEvcs.getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
                int minPower = this.minPower(currentEvcs);
                if (newPower >= minPower) {
                    currentEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.middleIndex || phaseConfiguration[1] == this.middleIndex) {
                        amountsLeft[1] -= amountToReduce;
                    }
                    this.log.info(currentEvcs.id() + " was reduced by " + amountToReduce * GRID_VOLTAGE//
                            + " W and is now at " + newPower * GRID_VOLTAGE + " W");
                }
                //If there exists an unbalanced load and the other two phases are both the minimum
            } else if (this.min2Index != 0) {
                int newPower;
                if (currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        currentEvcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
                    newPower = ((currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                            currentEvcs.getSetChargePowerLimitChannel().value().orElse(0))//
                            / GRID_VOLTAGE) - amountToReduce);
                } else {
                    newPower = (currentEvcs.getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
                }
                int minPower = this.minPower(currentEvcs);
                if (newPower >= minPower) {
                    currentEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                    amountsLeft[0] -= amountToReduce;
                    //If the second phase happens to be the one that also has to be reduced
                    if (phaseConfiguration[0] == this.middleIndex || phaseConfiguration[1] == this.middleIndex) {
                        amountsLeft[1] -= amountToReduce;
                    }
                    this.log.info(currentEvcs.id() + " was reduced by " + amountToReduce * GRID_VOLTAGE//
                            + " W and is now at " + newPower * GRID_VOLTAGE + " W");
                }
            }
        }
        return amountsLeft;
    }

    /**
     * Reduces the Power of EVCS that charge with three Phases by a amount given.
     *
     * @param threePhase       Array of all EVCS that have to be reduced
     * @param amountToReduce   Amount that has to be reduced per EVCS
     * @return modified amountsLeft
     * @throws OpenemsError.OpenemsNamedException This shouldn't happen
     */
    private int reduceThreePhaseEvcs(ManagedEvcs[] threePhase, int amountToReduce) throws
            OpenemsError.OpenemsNamedException {
        int amountReduced = 0;
        if (amountToReduce == 0) {
            amountToReduce = 1;
        }
        for (ManagedEvcs currentEvcs : threePhase) {
            if (currentEvcs.getIsPriority().get() && this.nonPriorityAmount > 0 //
                    && (!this.config.symmetry() && this.max - this.min < this.config.symmetryDelta())) {
                continue;
            }
            int newPower;
            if (currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    currentEvcs.getSetChargePowerLimitChannel().value().orElse(-1)) != -1) {
                newPower = ((currentEvcs.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        currentEvcs.getSetChargePowerLimitChannel().value().orElse(0))//
                        / GRID_VOLTAGE) - amountToReduce);
            } else {
                newPower = (currentEvcs.getChargePower().get() / GRID_VOLTAGE) - amountToReduce;
            }
            int minPower = this.minPower(currentEvcs);
            if (newPower >= (minPower)) {
                currentEvcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                amountReduced += amountToReduce;
            }
        }
        return amountReduced / 3;
    }


    //---------------------General Methods-----------------------\\

    /**
     * Returns if the 3 given Phase Power Values are balanced or not.
     *
     * @param max     Highest Power Value
     * @param middle  Middle Power Value
     * @param minimum Lowest Power Value
     * @return true if its Balanced
     */
    private boolean balance(int max, int middle, int minimum) {
        return (max - minimum < this.config.symmetryDelta() && middle - minimum < this.config.symmetryDelta());
    }

    /**
     * Creates Arrays of problematic EVCS and wraps them into another Array.
     *
     * @return Array (either 2 or 4 in length) of EVCS Arrays
     */
    private Optional<List<ManagedEvcs[]>> unbalancedEvcsOnPhase() {
        if (this.middleIndex != 0) {
            this.log.info("There exists an unbalanced load on Phases " + this.maxIndex + " and " + this.middleIndex);
        } else {
            this.log.info("There exists an unbalanced load on Phase " + this.maxIndex);
        }
        ManagedEvcs[] onePhase = this.getOnePhaseEvcs(this.maxIndex);
        ManagedEvcs[] twoPhase = this.getTwoPhaseEvcs(this.maxIndex);
        List<ManagedEvcs[]> output = new ArrayList<>();
        output.add(ONE_PHASE_INDEX, onePhase);
        output.add(TWO_PHASE_INDEX, twoPhase);
        //If there is an unbalanced load on 2 Phases
        if (this.middleIndex != 0) {
            ManagedEvcs[] onePhase2 = this.getOnePhaseEvcs(this.middleIndex);
            ManagedEvcs[] twoPhase2 = this.getTwoPhaseEvcs(this.middleIndex);
            output.add(ONE_PHASE_INDEX_2, onePhase2);
            output.add(TWO_PHASE_INDEX_2, twoPhase2);
        }
        return Optional.of(output);

    }


    /**
     * Puts all EVCS that charge with one Phase in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getOnePhaseEvcs() {
        List<ManagedEvcs> onePhaseList = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            if (managedEvcs.getPhases().orElse(0) == 1 //
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                onePhaseList.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(onePhaseList);
    }


    /**
     * Puts all EVCS that charge with one Phase on the unbalanced Phase in an Array.
     *
     * @param problemPhase Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getOnePhaseEvcs(int problemPhase) {
        List<ManagedEvcs> onePhaseList = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            int[] phaseConfiguration = managedEvcs.getPhaseConfiguration();
            if (managedEvcs.getPhases().orElse(0) == 1 && phaseConfiguration[0] == problemPhase//
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                onePhaseList.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(onePhaseList);
    }

    /**
     * Puts all EVCS that charge with two Phases in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs() {
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            if (managedEvcs.getPhases().orElse(0) == 2//
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                twoPhase.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(twoPhase);
    }

    /**
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
     *
     * @param problemPhase Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(int problemPhase) {
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            int[] phaseConfiguration = managedEvcs.getPhaseConfiguration();
            if (managedEvcs.getPhases().orElse(0) == 2 //
                    && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                twoPhase.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(twoPhase);
    }

    /**
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
     * This is an Expansion of the above Method.
     *
     * @param problemPhase  Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @param excludedPhase The Phase that should not be one of the two phases.
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(int problemPhase, int excludedPhase) {
        if (excludedPhase < 1) {
            excludedPhase = 3;
        }
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            int[] phaseConfiguration = managedEvcs.getPhaseConfiguration();
            if (managedEvcs.getPhases().orElse(0) == 2 //
                    && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)//
                    && (phaseConfiguration[0] != excludedPhase || phaseConfiguration[1] != excludedPhase)//
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                twoPhase.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(twoPhase);
    }

    /**
     * Puts all EVCS that charge with two Phases of which one is the unbalanced Phase in an Array.
     * This is an Expansion of the above Method.
     *
     * @param evcs          EVCS Array where all the applicable EVCS are in
     * @param problemPhase  Number of the Phase (Note: NOT number of Phases but instead the actual Number behind the L)
     * @param excludedPhase The Phase that should not be one of the two phases.
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getTwoPhaseEvcs(ManagedEvcs[] evcs, int problemPhase, int excludedPhase) {
        if (excludedPhase < 1) {
            excludedPhase = 3;
        }
        List<ManagedEvcs> twoPhase = new ArrayList<>();
        for (int i = 0; i < evcs.length; i++) {
            int[] phaseConfiguration = evcs[i].getPhaseConfiguration();
            if (evcs[i].getPhases().orElse(0) == 2 //
                    && (phaseConfiguration[0] == problemPhase || phaseConfiguration[1] == problemPhase)//
                    && (phaseConfiguration[0] != excludedPhase || phaseConfiguration[1] != excludedPhase)//
                    && !this.powerWaitingList.containsKey(this.evcss[i].id())) {
                twoPhase.add(evcs[i]);
            }
        }
        return this.convertListIntoArray(twoPhase);
    }

    /**
     * Puts all EVCS that charge with three Phases in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getThreePhaseEvcs() {
        List<ManagedEvcs> threePhase = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            if (managedEvcs.getPhases().orElse(0) == 3 //
                    && !this.powerWaitingList.containsKey(managedEvcs.id())) {
                threePhase.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(threePhase);
    }

    /**
     * Puts all EVCS in an Array.
     *
     * @return ManagedEvcs[]
     */
    private ManagedEvcs[] getEvcs() {
        List<ManagedEvcs> evcs = new ArrayList<>();
        for (ManagedEvcs managedEvcs : this.evcss) {
            if (!this.powerWaitingList.containsKey(managedEvcs.id())) {
                evcs.add(managedEvcs);
            }
        }
        return this.convertListIntoArray(evcs);
    }


    /**
     * This converts a ManagedEVCS List into in Array since its impossible to cast.
     *
     * @param phaseList List of ManagedEvcs
     * @return Array of the same ManagedEvcs
     */
    private ManagedEvcs[] convertListIntoArray(List<ManagedEvcs> phaseList) {
        ManagedEvcs[] output = new ManagedEvcs[phaseList.size()];
        for (int i = 0; i < phaseList.size(); i++) {
            output[i] = phaseList.get(i);
        }
        return output;
    }

    /**
     * Returns the minimum between MinimumHardwarePower and MinimumPower of an Evcs.
     * @param managedEvcs Evcs which minimumPowerValue is being searched for
     * @return Math.min(minimumHwPower,minimumPower)
     */
    private int minPower(ManagedEvcs managedEvcs) {
        int minHwPower = managedEvcs.getMinimumHardwarePower().orElse(MINIMUM_POWER);
        int minSwPower = managedEvcs.getMinimumPower().orElse(MINIMUM_POWER);
        return Math.max(minHwPower, minSwPower) / GRID_VOLTAGE;
    }

    /**
     * Update the Array of EVCSS.
     */
    private void updateEvcss() throws ConfigurationException {
        try {
            for (int i = 0; i < this.ids.length; i++) {
                OpenemsComponent component = this.cpm.getComponent(this.ids[i]);
                if (component instanceof ManagedEvcs) {
                    this.evcss[i] = (ManagedEvcs) component;
                } else {
                    throw new ConfigurationException("The EVCSsId list contains a wrong ID: ", this.ids[i] //
                            + " is not a EVCS");
                }
            }
        } catch (Exception e) {
            this.log.info("Unable to find Component. OpenEms is either still starting or the Name is incorrect.");
            this.evcss = new ManagedEvcs[this.ids.length];
        }
    }


    /**
     * Detects the Maximum Phase/s and stores the information in this Object ( max,max2,maxIndex,max2Index ).
     *
     * @return The highest Power Consumption of all Phases.
     */
    private int getMaximumLoad() {
        this.max = 0;
        this.maxIndex = 0;
        this.middleIndex = 0;
        int max = Math.max(Math.max(this.currentL1, this.currentL2), this.currentL3);
        if (max == this.currentL1) {
            this.max = this.currentL1;
            this.maxIndex = 1;
        }
        if (max == this.currentL2) {
            if (this.max == 0) {
                this.max = this.currentL2;
                this.maxIndex = 2;
            } else {
                this.middleIndex = 2;
            }
        }
        if (max == this.currentL3) {
            if (this.max == 0) {
                this.max = this.currentL3;
                this.maxIndex = 3;
            } else {
                this.middleIndex = 3;
            }
        }
        return this.max;
    }

    /**
     * Returns the Phase that is in the middle of the other two phases IF its not the same as one of the other.
     * This is necessary if a new higher power is applied, to validate if the power of the middle Phase is over a limit.
     *
     * @return The middle Power Consumption of all Phases
     */
    private int getMiddleLoad() {
        this.middle = 0;
        if (this.currentL1 > this.currentL2) {
            if (this.currentL2 > this.currentL3) {
                this.middleIndex = 2;
                this.middle = this.currentL2;
            } else if (this.currentL3 > this.currentL2) {
                if (this.currentL1.equals(this.currentL3)) {
                    this.middleIndex2 = 2;
                }
                if (this.currentL1>this.currentL3) {
                    this.middleIndex = 3;
                    this.middle = this.currentL3;
                } else {
                    this.middleIndex = 1;
                    this.middle = this.currentL1;
                }
            }
        } else if (this.currentL1 > this.currentL3) {
            if (this.currentL1.equals(this.currentL2)) {
                this.middleIndex2 = 2;
            }
            this.middleIndex = 1;
            this.middle = this.currentL1;

        } else if (this.currentL3 > this.currentL1) {
            if (this.currentL3 > this.currentL2) {
                if (this.currentL1.equals(this.currentL2)) {
                    this.middleIndex2 = 1;
                }
                this.middleIndex = 2;
                this.middle = this.currentL2;
            } else if (this.currentL2 > this.currentL3) {
                this.middleIndex = 3;
                this.middle = this.currentL3;
            } else {
                this.middleIndex = 2;
                this.middleIndex2 = 3;
            }
        }
        return this.middle;
    }

    /**
     * Detects the minimum Power Consumption.
     *
     * @return The lowest Power Consumption of all Phases.
     */
    private int getMinimumLoad() {
        this.min = 0;
        this.minIndex = 0;
        this.min2Index = 0;
        int min = Math.min(Math.min(this.currentL1, this.currentL2), this.currentL3);
        if (min == this.currentL1) {
            this.min = this.currentL1;
            this.minIndex = 1;
        }
        if (min == this.currentL2) {
            if (this.min == 0) {
                this.min = this.currentL2;
                this.minIndex = 2;
            } else {
                this.min2Index = 2;
            }
        }
        if (min == this.currentL3) {
            if (this.min == 0) {
                this.min = this.currentL3;
                this.minIndex = 3;
            } else {
                this.min2Index = 3;
            }
        }
        return this.min;
    }

    /**
     * Returns the index of the Phase the current Power value is on.
     *
     * @param power Power on a phase
     * @return Index or 0 if the power is not on any phase
     */
    private int getPhaseByPower(int power) {
        if (this.currentL1 == power) {
            return 1;
        } else if (this.currentL2 == power) {
            return 2;
        } else if (this.currentL3 == power) {
            return 3;
        }
        return 0;
    }

    /**
     * Returns the index of the Phase the current Power value is on.
     *
     * @param phasePower1 L1
     * @param phasePower2 L2
     * @param phasePower3 L3
     * @param power       Power on a phase
     * @return Index or 0 if the power is not on any phase
     */
    private int getPhaseByPower(int phasePower1, int phasePower2, int phasePower3, int power) {
        if (phasePower1 == power) {
            return 1;
        } else if (phasePower2 == power) {
            return 2;
        } else if (phasePower3 == power) {
            return 3;
        }
        return 0;
    }

    /**
     * Updated the Stored Power Values of the Phases.
     * If there is a meter for symmetry, the offset will also be calculated on top of that.
     *
     * @param tempered Was the power already changed in this cycle in some way?
     */
    private void updatePower(boolean tempered) {
        this.currentL1 = 0;
        this.currentL2 = 0;
        this.currentL3 = 0;
        //Updates current Power Consumption
        for (ManagedEvcs target : this.evcss) {
            //target._setIsClustered(true);
            int[] phases = target.getPhaseConfiguration();
            int phaseCount = target.getPhases().orElse(1);
            for (int n = 0; n < phaseCount; n++) {
                if (tempered && target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        target.getSetChargePowerLimitChannel().value().orElse(-1)) != -1//
                        && target.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                        target.getSetChargePowerLimitChannel().value().orElse(//
                                0)) <= target.getChargePower().orElse(//
                        target.getChargePowerChannel().getNextValue().orElse(0)) / phaseCount) {
                    this.updatePowerValue(n, phases, phaseCount, target.getSetChargePowerLimitChannel());
                } else {
                    this.updatePowerValue(n, phases, phaseCount, target.getChargePowerChannel());
                }
            }
        }
        int l1Offset = 0;
        int l2Offset = 0;
        int l3Offset = 0;
        //If the Symmetry has to be determined based on a preexisting meter, the offset will be calculated now
        if (this.config.useMeter() && this.symmetryMeter != null && this.currentL1 != null) {
            this.evcsL1 = this.currentL1;
            this.evcsL2 = this.currentL2;
            this.evcsL3 = this.currentL3;
            int testPowerL1 = this.symmetryMeter.getActivePowerL1().orElse(0) / GRID_VOLTAGE;
            int testPowerL2 = this.symmetryMeter.getActivePowerL2().orElse(0) / GRID_VOLTAGE;
            int testPowerL3 = this.symmetryMeter.getActivePowerL3().orElse(0) / GRID_VOLTAGE;
            int l1 = Math.abs(testPowerL1);
            int l2 = Math.abs(testPowerL2);
            int l3 = Math.abs(testPowerL3);
            int maxPower = Math.abs(Math.min(Math.min(testPowerL1, testPowerL2), testPowerL3));
            l1Offset = (this.currentL1 - l1) - maxPower;
            if (l1Offset < 0) {
                l1Offset = 0;
            }
            l2Offset = (this.currentL2 - l2) - maxPower;
            if (l2Offset < 0) {
                l2Offset = 0;
            }
            l3Offset = (this.currentL3 - l3) - maxPower;
            if (l3Offset < 0) {
                l3Offset = 0;
            }
            this.l1Offs = l1Offset;
            this.l2Offs = l2Offset;
            this.l3Offs = l3Offset;
            this.currentL1 += Math.abs(l1Offset);
            this.currentL2 += Math.abs(l2Offset);
            this.currentL3 += Math.abs(l3Offset);
        }
    }

    /**
     * Updates the internal Power based on the predefined Channel.
     * @param phase Number of the current phase
     * @param phases Phase configuration (eg 1,2,3)
     * @param phaseCount Amount of phases the Evcs is charging with
     * @param channel where the power value has to be read from
     */
    private void updatePowerValue(int phase, int[] phases, int phaseCount, IntegerReadChannel channel) {
        switch (phases[phase]) {
            case 1:
                this.currentL1 += (int) Math.round((channel.value().orElse(//
                        channel.getNextValue().orElse(0))//
                        / (GRID_VOLTAGE * 1.0)) / (phaseCount * 1.0));
                break;
            case 2:
                this.currentL2 += (int) Math.round((channel.value().orElse(//
                        channel.getNextValue().orElse(0))//
                        / (GRID_VOLTAGE * 1.0)) / (phaseCount * 1.0));
                break;
            case 3:
                this.currentL3 += (int) Math.round((channel.value().orElse(//
                        channel.getNextValue().orElse(0))//
                        / (GRID_VOLTAGE * 1.0)) / (phaseCount * 1.0));
                break;
        }
    }

    /**
     * Checks if a EVCS is active even though its on the waiting list ( e.g a new car plugged in )
     * and checks if an EVCS has a power of 0 and isn't on the waiting list.
     */
    private void checkWaitingList() {
        for (ManagedEvcs managedEvcs : this.evcss) {
            if (managedEvcs != null) {
                if (this.powerWaitingList.containsKey(managedEvcs.id())
                        && (this.getPower(managedEvcs) >= 1 || managedEvcs.getSetChargePowerRequest().isDefined()//
                        || managedEvcs.getSetChargePowerRequestChannel().getNextWriteValue().isPresent()
                )
                ) {
                    this.powerWaitingList.remove(managedEvcs.id());
                    try {
                        if (this.getCurrentPowerChannel().getNextValue().orElse(0)//
                                + managedEvcs.getMinimumPower().orElse(MINIMUM_CURRENT * GRID_VOLTAGE) <= this.powerLimit) {

                            managedEvcs.setChargePowerLimit(managedEvcs.getMinimumPower().orElse(MINIMUM_CURRENT * GRID_VOLTAGE));
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        //
                    }
                } else if (!this.powerWaitingList.containsKey(managedEvcs.id()) && this.getPower(managedEvcs)
                        < 1
                ) {
                    int minPower = this.minPower(managedEvcs);
                    this.powerWaitingList.put(managedEvcs.id(),//
                            new EvcsOnHold(minPower, Instant.now(),//
                                    managedEvcs.getPhases().orElse(0), false));
                    this.removeEvcsFromActive(managedEvcs);
                }
            }
        }
    }

    /**
     * Checks if the offTime for an EVCS on the waiting list is over to put them back on the list.
     * In order to turn off one Evcs, it will turn one Active Evcs off.
     * It needs to check if the active power is higher
     * or the same as the minimal Power of the waiting Evcs to turn it on.
     */
    private void swapWaitingEvcs() {
        AtomicReference<Boolean> allOff = new AtomicReference<>(true);
        List<String> remove = new ArrayList<>();
        Map<String, EvcsOnHold> add = new HashMap<>();

        AtomicReference<List<ManagedEvcs>> active = new AtomicReference<>(this.nonPriorityList);
        AtomicInteger activeLength = new AtomicInteger(active.get().size());
        Instant current = Instant.now();
        this.powerWaitingList.forEach((id, evcs) -> {
            if (this.canActivate(id, evcs)) {
                Instant time = evcs.getTimestamp();
                active.set(this.nonPriorityList);
                if (activeLength.get() == 0 && allOff.get()
                       // && this.canActivate(id, evcs)
                ) {
                    remove.add(id);
                    allOff.set(false);

                } else {
                    try {
                        ManagedEvcs temp = this.cpm.getComponent(id);

                        if (time.plus(this.config.offTime(),ChronoUnit.MINUTES).isBefore(current)
                                || (temp.getIsPriority().get()
                                && this.nonPriorityAmount > 0)
                        ) {
                            for (int i = 0; i < activeLength.get(); i++) {
                                int waitingPower = evcs.getCurrent();
                                int currentPower = active.get().get(i).getChargePower().orElse(0);
                                // Checks if the active Evcs currently pointed at has a higher power then the minimal power the waiting Evcs needs.
                                if (currentPower >= waitingPower * GRID_VOLTAGE) {
                                    remove.add(id);
                                    this.swapped = true;
                                    this.removeEvcsFromActive(active.get().get(i));
                                    //Adds the active Evcs to the waitingList with minimal power
                                    int minPower = this.minPower(active.get().get(i));
                                    add.put(active.get().get(i).id(), //
                                            new EvcsOnHold(minPower, current, active.get().get(i).getPhases().orElse(OFF), true));
                                    try {
                                        this.turnOffEvcs(active.get().get(i));
                                        this.turnOnEvcs(id, evcs);
                                    } catch (OpenemsError.OpenemsNamedException e) {
                                        this.log.error("Couldn't turn off EVCS.");
                                    }
                                    activeLength.getAndDecrement();
                                }
                            }
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.error("Error in SwapWaitingList");
                    }
                }
            }
        });
        remove.forEach(this.powerWaitingList::remove);
        this.powerWaitingList.putAll(add);
    }


    /**
     * Checks if the EVCS on the waiting list can activate under the given Limits, if no EVCS is active.
     *
     * @param id   Id of the EVCS on hold
     * @param evcs The evcs on hold from the forEach
     * @return true if that is the case
     */
    private boolean canActivate(String id, EvcsOnHold evcs) {
        int minPower = MINIMUM_CURRENT;
        try {
            ManagedEvcs temp = this.cpm.getComponent(id);
            minPower = this.minPower(temp);
        } catch (OpenemsError.OpenemsNamedException ignored) {
            this.log.error("Error in canActive. This should not have happened.");
        }

        if (this.powerLimit == 0 && this.phaseLimit != 0) {
            return minPower <= (this.phaseLimit / GRID_VOLTAGE);
        }
        if (this.phaseLimit == 0 && this.powerLimit != 0) {
            return this.powerLimit / GRID_VOLTAGE > minPower;
        } else {
            return minPower <= (this.phaseLimit / GRID_VOLTAGE) && this.powerLimit / GRID_VOLTAGE > minPower;
        }
    }

    /**
     * Checks if there are Power Resources free and reallocates them to the EVCS.
     * TODO: If something goes wrong, this is usually the culprit.
     * Priority List:
     * 1. EVCS on the waiting list
     * 2. All EVCS evenly (only if the waiting list is empty)
     * All of these functionalities can be offset by a timer (smooth_time)
     */
    private void reallocateFreeResources() {
        if (this.currentL1 != null && this.currentL2 != null && this.currentL3 != null && (this.phaseLimit != 0 || this.powerLimit != 0)) {
            if (!this.swapped) {
                //Determines how much Amperage is free until the closest limit is reached
                int powerSum = this.currentL1 + this.currentL2 + this.currentL3;
                int freeResourcesPerPhase = Math.abs((this.phaseLimit / GRID_VOLTAGE) - (this.getMaximumLoad()));
                int freeResourcesFromGrid = Math.abs((this.powerLimit / GRID_VOLTAGE) - powerSum);
                int freeResources;
                if (this.powerLimit == 0) {
                    freeResources = freeResourcesPerPhase;
                } else if (this.phaseLimit == 0) {
                    freeResources = freeResourcesFromGrid;
                } else {
                    freeResources = Math.min(freeResourcesPerPhase, freeResourcesFromGrid);
                    if (this.config.symmetry()) {
                        //delta is the difference between the highest Phase and the maximum allowed difference of the unbalanced load
                        int delta = this.config.symmetryDelta() - (this.getMaximumLoad() - this.getMinimumLoad());
                        if (delta <= freeResources) {
                            freeResources = delta - 1;
                            if (freeResources < 0) {
                                freeResources = 0;
                            }
                        }
                    }
                }

/*
                if (this.time.checkTimeIsUp(ONE_MINUTE) && this.config.deltaTime() >=1) {
                    this.smoothAverageList.add(freeResources);
                    this.time.resetTimer(ONE_MINUTE);
                }
                if (this.config.deltaTime() <= 1 || this.time.checkTimeIsUp(SMOOTH_TIME)) {
                        this.time.resetTimer(SMOOTH_TIME);
                        if (this.config.deltaTime() >= 1) {
                        int smoothResources = 0;
                        for (Integer smoothAverage : this.smoothAverageList) {
                            smoothResources += smoothAverage;
                        }
                        if (this.smoothAverageList.size() != 0) {
                            smoothResources = smoothResources / this.smoothAverageList.size();
                            this.smoothAverageList.clear();
                                freeResources = Math.min(freeResources, smoothResources);
                        }
                    }
*/
                    if (freeResources > 0) {

                        //Checks if there is enough Amperage to activate a Evcs on the waiting list
                        if (freeResources > MINIMUM_CURRENT) {

                            //-----------Reallocate to the waitingList------------\\
                            int n = 0;
                            AtomicReference<List<String>> tested = new AtomicReference<>(new ArrayList<>());
                            while (freeResources > MINIMUM_CURRENT && !this.powerWaitingList.isEmpty()) {
                                n++;
                                AtomicReference<String> waitingId = new AtomicReference<>();
                                AtomicReference<Instant> waitingTime = new AtomicReference<>();
                                AtomicInteger waitingPhases = new AtomicInteger();
                                AtomicInteger waitingPower = new AtomicInteger();
                                AtomicBoolean waitingWantToCharge = new AtomicBoolean(false);
                                AtomicBoolean foundAEvcsToTurnOn = new AtomicBoolean(false);
                                int finalFreeResources1 = freeResources;
                                //Finds an Evcs on the waiting list that can be activated
                                this.powerWaitingList.forEach((id, evcs) -> {
                                    //TODO Choose Random member from the list and not always the same. This may be stupid but idk.
                                    if ((waitingTime.get() == null || evcs.getTimestamp().isBefore(waitingTime.get()))//
                                            && !tested.get().contains(id) && !foundAEvcsToTurnOn.get()) {
                                        waitingTime.set(evcs.getTimestamp());
                                        waitingPhases.set(evcs.getPhases());
                                        waitingId.set(id);
                                        waitingPower.set(evcs.getCurrent());
                                        foundAEvcsToTurnOn.set(true);
                                        try {
                                            ManagedEvcs target = this.cpm.getComponent(id);
                                            waitingWantToCharge.set(evcs.getWantToCharge() //
                                                    || target.getSetChargePowerRequest().isDefined() //
                                                    || target.getStatus().equals(Status.CHARGING));
                                        } catch (OpenemsError.OpenemsNamedException e) {
                                            waitingWantToCharge.set(evcs.getWantToCharge());
                                        }
                                    }
                                });
                                //Activates the Evcs
                                try {
                                    if (waitingId.get() == null) {
                                        break;
                                    }
                                    ManagedEvcs evcs = this.cpm.getComponent(waitingId.get());
                                    if (evcs.getChargePower().orElse(0) > 1 || waitingPower.get() > 1) {

                                        if (freeResources >= waitingPower.get() && (waitingWantToCharge.get() //
                                                && waitingPower.get() //
                                                >= (Math.min(evcs.getMinimumHardwarePower().get(), evcs.getMinimumPower().orElse(MINIMUM_POWER)) / GRID_VOLTAGE))) {
                                            evcs.setChargePowerLimit(waitingPower.get() * GRID_VOLTAGE);
                                            freeResources -= waitingPower.get();
                                            this.powerWaitingList.remove(waitingId.get());
                                        } else if (freeResources >= MINIMUM_CURRENT * evcs.getPhases().orElse(3)//
                                                && waitingWantToCharge.get()) {
                                            evcs.setChargePowerLimit(MINIMUM_CURRENT * evcs.getPhases().orElse(3) * GRID_VOLTAGE);
                                            freeResources -= MINIMUM_CURRENT * evcs.getPhases().orElse(3);
                                            this.powerWaitingList.remove(waitingId.get());
                                        } else {
                                            List<String> add = tested.get();
                                            add.add(waitingId.get());
                                            tested.set(add);
                                        }
                                    }

                                } catch (OpenemsError.OpenemsNamedException e) {
                                    this.log.error("Not an EVCS.");
                                }
                                if (n > this.powerWaitingList.size()) {
                                    break;
                                }
                            }
                        }
                    /*
                    else {
                        int waitingEvcs = this.powerWaitingList.size();
                        int i = 0;
                        while (freeResources > 2 && this.powerWaitingList.isEmpty() == false) {
                            i++;
                            AtomicReference<String> waitingId = new AtomicReference<>();
                            AtomicReference<DateTime> waitingTime = new AtomicReference<>();
                            AtomicInteger waitingPhases = new AtomicInteger();
                            AtomicBoolean waitingWantToCharge = new AtomicBoolean(false);
                            int finalFreeResources = freeResources;
                            this.powerWaitingList.forEach((id, evcs) -> {
                                int resourceReduction = Math.floorDiv(finalFreeResources, evcs.getPhases());
                                if (((resourceReduction > 0 && evcs.getPhases() > 1) || evcs.getPhases() == 1 && resourceReduction > 6)
                                        && (waitingTime.get() == null || evcs.getTimestamp().isBefore(waitingTime.get()))) {
                                    waitingTime.set(evcs.getTimestamp());
                                    waitingPhases.set(evcs.getPhases());
                                    waitingId.set(id);
                                    try {
                                        ManagedEvcs target = this.cpm.getComponent(id);
                                        waitingWantToCharge.set(evcs.getWantToCharge() //
                                        || target.getSetChargePowerRequest().isDefined() || target.getStatus().equals(Status.CHARGING));
                                    } catch (OpenemsError.OpenemsNamedException e) {
                                        waitingWantToCharge.set(evcs.getWantToCharge());
                                    }
                                }
                            });
                            try {
                                if (waitingId.get() == null) {
                                    break;
                                }
                                ManagedEvcs evcs = this.cpm.getComponent(waitingId.get());

                                int resourceReduction = Math.floorDiv(freeResources, waitingPhases.get());
                                if (resourceReduction > 0 && resourceReduction //
                                >= Math.min(evcs.getMinimumHardwarePower().orElse(99), evcs.getMinimumPower().orElse(99))
                                        && (evcs.getChargePower().get() > 1) || waitingWantToCharge.get()) {
                                    this.powerWaitingList.remove(waitingId.get());
                                    evcs.setChargePowerLimit(resourceReduction * GRID_VOLTAGE);
                                    freeResources -= resourceReduction;
                                } else {
                                    if (i > waitingEvcs) {
                                        break;
                                    }
                                }
                            } catch (OpenemsError.OpenemsNamedException e) {
                                this.log.error("Not an EVCS.");
                            }
                        }
                    }
                    */
                        //-----------Reallocate to everyone else-------------\\
                        this.setFreePower(freeResources);

                        if (freeResources > 0) {
                            ManagedEvcs[] everyone = this.active.stream().filter(//
                                    evcs -> !this.powerWaitingList.containsKey(evcs.id())).toArray(ManagedEvcs[]::new);

                            if (everyone.length != 0) {
                                this.setActive(everyone.length);
                                int powerForEveryone = freeResources / everyone.length;
                                //The SmoothAssign can be configured and is the minimal Amount the Limiter has to assign to smooth out the Power
                                if (powerForEveryone >= this.config.minimumAssign()) {
                                    this.increasePowerBy(powerForEveryone, everyone);
                                } else {
                                    while (freeResources >= this.config.minimumAssign()) {
                                        //The first comparison should always be less than the comparator, so it is initialized with an absurdly large number
                                        AtomicInteger comparator = new AtomicInteger(Integer.MAX_VALUE);
                                        AtomicReference<ManagedEvcs> leastChargeAmount = new AtomicReference<>();
                                        Arrays.stream(everyone).sequential().forEach(evcs -> {
                                            int power = Math.min(evcs.getChargePower().orElse(Integer.MAX_VALUE), evcs.getSetChargePowerLimit().orElse(Integer.MAX_VALUE));
                                            if (power < comparator.get()) {
                                                leastChargeAmount.set(evcs);
                                                comparator.set(power);
                                            }
                                        });
                                        if (comparator.get() != Integer.MAX_VALUE) {
                                            this.increasePowerBy(this.config.minimumAssign(), new ManagedEvcs[]{leastChargeAmount.get()});
                                            freeResources -= this.config.minimumAssign();
                                        }
                                    }
                                }
                            }
                        }

                    }
                    this.setFreePower(freeResources);
                } else {
                    this.swapped = false;
                }
            }
        //}
    }

    /**
     * Increases the Power of All ManagedEVCS in a given Array by a given amount.
     *
     * @param powerForEveryone Power that should be added everywhere
     * @param evcss            Array of all ManagedEvcs
     */
    private void increasePowerBy(int powerForEveryone, ManagedEvcs[] evcss) {

        for (int i = 0; i < evcss.length; i++) {
            int newIncreaseAmount = 0;
            boolean allow = !evcss[i].getIsPriority().get() && !this.powerWaitingList.containsKey(evcss[i].id());
            boolean overLimit = ((this.getMaximumLoad() - this.getMinimumLoad()) + powerForEveryone >= this.config.symmetryDelta()
                    || this.getMiddleLoad() - this.getMinimumLoad() + powerForEveryone >= this.config.symmetryDelta());
            if (overLimit) {
                newIncreaseAmount = (this.config.symmetryDelta() - (this.getMaximumLoad() - this.getMinimumLoad())) / evcss.length;
                allow = false;
            }
            int[] phaseConfiguration = evcss[i].getPhaseConfiguration();
            int phaseCount = evcss[i].getPhases().orElse(0);
            if (evcss[i].getChargePower().get() + powerForEveryone //
                    < (Math.min(evcss[i].getMinimumHardwarePower().orElse(MINIMUM_POWER), evcss[i].getMinimumPower().orElse(MINIMUM_POWER)) / GRID_VOLTAGE)) {
                allow = false;
            }
            switch (phaseCount) {
                case 1:
                    if ((phaseConfiguration[0] == this.maxIndex)
                            && overLimit) {
                        allow = false;
                    }
                    break;
                case 2:
                    if ((phaseConfiguration[0] == this.maxIndex || phaseConfiguration[1] == this.maxIndex)
                            && overLimit) {
                        allow = false;
                    }
                    break;
                case 3:
                    break;

            }
            if (allow) {
                int oldPower = this.getPower(evcss[i]);
                int newPower = oldPower + powerForEveryone;
                try {
                    if (phaseCount == 3 || (newPower - this.getMinimumLoad() <= this.config.symmetryDelta() &&
                            newPower >= (Math.min(evcss[i].getMinimumHardwarePower().orElse(MINIMUM_POWER),
                                    evcss[i].getMinimumPower().orElse(MINIMUM_POWER)) / GRID_VOLTAGE))) {
                        evcss[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    } else {
                        evcss[i].setChargePowerLimit((oldPower + (this.config.symmetryDelta() - oldPower)) * GRID_VOLTAGE);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Couldn't increase Power for " + evcss[i].id());
                }
            } else if (newIncreaseAmount > 0) {
                int oldPower = this.getPower(evcss[i]);
                int newPower = oldPower + newIncreaseAmount;
                try {
                    if (newPower - this.getMinimumLoad() <= this.config.symmetryDelta() && newPower >= (Math.min(evcss[i].getMinimumHardwarePower().get(), evcss[i].getMinimumPower().get()) / GRID_VOLTAGE)) {
                        evcss[i].setChargePowerLimit(newPower * GRID_VOLTAGE);
                    }
                } catch (OpenemsError.OpenemsNamedException e) {
                    this.log.error("Couldn't increase Power for " + evcss[i].id());
                }
            }
        }
    }

    /**
     * Returns the Power of an ManagedEVCS.
     *
     * @param evcss Evcs which power is needed
     * @return Power of that EVCS
     */
    private int getPower(ManagedEvcs evcss) {
        if (evcss.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                evcss.getSetChargePowerLimitChannel().value().orElse(-1)) != -1
                && evcss.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                evcss.getSetChargePowerLimitChannel().value().orElse(//
                        0)) <= evcss.getChargePower().orElse(0)) {
            return (evcss.getSetChargePowerLimitChannel().getNextWriteValue().orElse(//
                    evcss.getSetChargePowerLimitChannel().value().orElse(0)) / GRID_VOLTAGE);
        } else {
            return (evcss.getChargePower().orElse(0) / GRID_VOLTAGE);
        }
    }

    /**
     * Removes an EVCS from an ManagedEvcs[] if its on the waitingList.
     *
     * @param evcs the ManagedEvcs[]
     * @return modified ManagedEvcs[]
     */
    private ManagedEvcs[] removeEvcsFromArray(ManagedEvcs[] evcs) {
        List<ManagedEvcs> output = new ArrayList<>();
        for (int i = 0; i < evcs.length; i++) {
            if (!this.powerWaitingList.containsKey(evcs[i].id())) {
                output.add(evcs[i]);
            }
        }
        return this.convertListIntoArray(output);
    }

    /**
     * Returns if an EVCS is charging on the specified Phase.
     *
     * @param evcs  Evcs that is examined
     * @param phase phase number that has to be tested
     * @return true if on the phase
     */
    private boolean evcsOnPhase(ManagedEvcs evcs, int phase) {
        int[] phaseConfiguration = evcs.getPhaseConfiguration();
        int phaseCount = evcs.getPhases().orElse(0);
        for (int n = 0; n < phaseCount; n++) {
            if (phaseConfiguration[n] == phase) {
                return true;
            }
        }
        return false;
    }

    /**
     * Reallocates the current resources to the priority EVCS if they don't charge with full power.
     * In case there is not enough current available, they will share the priority current instead.
     * First it calculates if there is enough current for all priority Evcs by dividing the PowerLimit with the amount of priority Evcs.
     * Afterwards it checks if there is any priority Evcs currently powered off and will reallocate the priority current amount to all priority Evcs.
     */
    private void reallocateToPriority() {
        List<ManagedEvcs> all = Arrays.asList(this.evcss);
        int freeResources;
        int freeMiddleResources;
        int freeResourcesPerEvcs = 0;
        int freeResourcesPerMiddleEvcs = 0;
        int nonThreePhasePriorityAmount = (int) this.priorityList.stream().filter(//
                evcs -> evcs.getPhases().orElse(0) != 3).count();
        int max = this.getMaximumLoad();
        int mid = this.getMiddleLoad();
        if (mid == 0) {
            mid = max;
        }
        int min = this.getMinimumLoad();
        if (this.priorityAmount > 0) {
            if ((this.config.powerLimit() / GRID_VOLTAGE) / this.priorityAmount > this.config.priorityCurrent()) {
                //In Case the Limiter has to watch out for the unbalanced load, it needs to adapt the new Power by checking with the unbalanced load delta
                if (this.config.symmetry()) {
                    freeResources = this.config.symmetryDelta() - (max - min);
                    freeMiddleResources = this.config.symmetryDelta() - (mid - min);
                    if (nonThreePhasePriorityAmount != 0) {
                        freeResourcesPerEvcs = (freeResources / nonThreePhasePriorityAmount) - 1;
                        freeResourcesPerMiddleEvcs = (freeMiddleResources / nonThreePhasePriorityAmount) - 1;
                    }
                }
                int finalFreeResourcesPerEvcs = freeResourcesPerEvcs;
                int finalFreeResourcesPerMiddleEvcs = freeResourcesPerMiddleEvcs;
                this.priorityList.forEach(evcs -> {
                    try {
                        if ((this.powerWaitingList.containsKey(evcs.id()) //
                                && this.powerWaitingList.get(evcs.id()).getWantToCharge()) //
                                || (!this.powerWaitingList.containsKey(evcs.id()) && evcs.getChargePower().orElse(0) > 0)) {
                            if (!this.config.symmetry() || evcs.getPhases().orElse(0) == 3) {
                                evcs.setChargePowerLimit(Math.min(evcs.getMaximumHardwarePower().orElse(//
                                        this.config.priorityCurrent()), evcs.getMaximumPower().orElse(this.config.priorityCurrent())));
                            } else {
                                int oldPower = evcs.getChargePower().orElse(0);
                                int minPower = Math.max(evcs.getMinimumHardwarePower().orElse(//
                                        0), evcs.getMinimumPower().orElse(0)) / GRID_VOLTAGE;
                                //Checks if the Priority Evcs is on the Phase with the highest Current,
                                //the additional current is high enough and if it is higher then the minimal amount the Evcs needs.
                                if (this.evcsOnPhase(evcs, this.maxIndex) //
                                        && finalFreeResourcesPerEvcs > 0 && oldPower //
                                        + (finalFreeResourcesPerEvcs * GRID_VOLTAGE) >= minPower) {
                                    evcs.setChargePowerLimit(oldPower + (finalFreeResourcesPerEvcs * GRID_VOLTAGE));
                                    //Same idea but on the phase with the second highest or highest if two phases are the same high amount
                                } else if ((this.evcsOnPhase(evcs, this.middleIndex) //
                                        || (this.maxIndex == this.middleIndex //
                                        && this.evcsOnPhase(evcs, this.middleIndex2))) //
                                        && !this.evcsOnPhase(evcs, this.maxIndex) //
                                        && finalFreeResourcesPerMiddleEvcs > 0 //
                                        && oldPower + (finalFreeResourcesPerMiddleEvcs * GRID_VOLTAGE) >= minPower) {
                                    evcs.setChargePowerLimit(oldPower + (finalFreeResourcesPerMiddleEvcs * GRID_VOLTAGE));
                                } else if (this.evcsOnPhase(evcs, this.minIndex) && !this.evcsOnPhase(evcs, this.maxIndex)) {
                                    evcs.setChargePowerLimit(//
                                            Math.min(evcs.getMaximumHardwarePower().orElse(this.config.priorityCurrent()), //
                                                    evcs.getMaximumPower().orElse(this.config.priorityCurrent())));
                                }
                            }
                        } else {
                            this.turnOffEvcs(evcs);
                        }
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.error("Unable to set ChargeLimit of Priority EVCS!");
                    }
                });
                //If there is not enough current so every priority Evcs can get the priority current,
                //they will share the priority current or will evenly divide how much current is in the system.
            } else {
                int minCurrent = Math.min(this.config.priorityCurrent(), (this.powerLimit / GRID_VOLTAGE));
                int newPower = minCurrent / this.priorityAmount;
                this.priorityList.forEach(evcs -> {
                    try {
                        evcs.setChargePowerLimit(newPower * GRID_VOLTAGE);
                    } catch (OpenemsError.OpenemsNamedException e) {
                        this.log.error("Unable to set ChargeLimit of Priority EVCS!");
                    }
                });
            }
        }

    }

    /**
     * Adds all Active Evcs to the Active list.
     */
    private void getActiveEvcss() {
        ManagedEvcs[] all = this.getEvcs();
        this.active = new ArrayList<>();
        this.active.addAll(Arrays.asList(all));
        this.priorityList = this.active.stream().filter(test -> test.getIsPriority().get()).collect(Collectors.toList());
        this.nonPriorityList = (this.active.stream().filter(test -> !test.getIsPriority().get())).collect(Collectors.toList());
        this.priorityAmount = this.priorityList.size();
        this.nonPriorityAmount = this.nonPriorityList.size();
    }

    /**
     * Removes an Evcs from the Active Evcs list.
     *
     * @param remove The Evcs that has to be removed from the active List
     * @return true if it was removed | false if it was not in the active list
     */
    private boolean removeEvcsFromActive(ManagedEvcs remove) {
        return this.active.remove(remove);
    }

    /**
     * Updates Power Limit based on the Connected Meter.
     */
    private void updatePowerLimit() {

        int limit = this.config.powerLimit();
        if (this.currentL1 != null && this.currentL2 != null && this.currentL3 != null) {
            //PV does not show in the system as a negative value but as a positive one.
            limit = this.config.powerLimit() - this.meter.getActivePower().orElse(0) //
                    + ((this.currentL1 + this.currentL2 + this.currentL3) * GRID_VOLTAGE);
        }
        if (limit <= 0) {
            this.setPowerLimit(1);
        } else {
            setPowerLimit(limit);
        }
    }

    /**
     * Updates the Current Power Channel with the Sum of the Phases.
     */
    private void updateChannel() {
        int powerSum = (this.currentL1 + this.currentL2 + this.currentL3) * GRID_VOLTAGE;
        this.setCurrentPower(powerSum - ((this.l1Offs + this.l2Offs + this.l3Offs) * GRID_VOLTAGE));
        this.setCurrentPowerWithOffset(powerSum);
    }


    /**
     * Turns an EVCSonHold on.
     *
     * @param id         ID of the EVCS
     * @param evcsOnHold the EVCSonHold
     * @throws OpenemsError.OpenemsNamedException If the ID doesn't belong to an EVCS. This shouldn't happen.
     */
    private void turnOnEvcs(String id, EvcsOnHold evcsOnHold) throws OpenemsError.OpenemsNamedException {
        ManagedEvcs evcs = this.cpm.getComponent(id);
        int newPower = evcsOnHold.getCurrent();
        if (newPower < MINIMUM_CURRENT * evcsOnHold.getPhases()) {
            newPower = MINIMUM_CURRENT * evcsOnHold.getPhases();
        }
        evcs.setChargePowerLimit(newPower * GRID_VOLTAGE);

    }


    /**
     * Stops all EVCS from consuming power.
     */
    private void emergencyStop() {
        for (int i = 0; i < this.evcss.length; i++) {
            try {
                this.evcss[i].setChargePowerLimit(OFF);
                if (!this.powerWaitingList.containsKey(this.evcss[i].id())) {
                    int minPower = this.minPower(this.evcss[i]);
                    this.powerWaitingList.put(this.evcss[i].id(), new EvcsOnHold(minPower, Instant.now(), //
                            this.evcss[i].getPhases().orElse(0), true));
                }
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.error("Unable to turn off all EVCS. Something went horribly wrong.");
            }
        }
    }

}