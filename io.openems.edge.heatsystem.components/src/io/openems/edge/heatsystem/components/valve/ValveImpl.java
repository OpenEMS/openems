package io.openems.edge.heatsystem.components.valve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.cycle.Cycle;
import io.openems.edge.common.event.EdgeEventConstants;

import io.openems.edge.heatsystem.components.ConfigurationType;
import io.openems.edge.heatsystem.components.HeatsystemComponent;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.relay.api.Relay;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;


/**
 * This Component allows a Valve  to be configured and controlled.
 * It either works with 2 Relays or 2 ChannelAddresses.
 * It updates it's opening/closing state and shows up the percentage value of itself.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "HeatsystemComponent.Valve",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE,
                EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)
public class ValveImpl extends AbstractOpenemsComponent implements OpenemsComponent, Valve, EventHandler {

    @Reference
    Cycle cycle;

    private final Logger log = LoggerFactory.getLogger(ValveImpl.class);

    private ChannelAddress openAddress;
    private ChannelAddress closeAddress;

    private Relay openRelay;
    private Relay closeRelay;

    private double secondsPerPercentage;
    private long timeStampValveInitial = 0;
    private long timeStampValveCurrent = -1;
    private boolean isChanging = false;
    //if true --> subtraction in updatePowerLevel else add
    private boolean isClosing = false;
    private boolean wasAlreadyReset = false;
    private boolean isForced;
    //Extra Buffer Time , only needed for Force Open/Close --> Just making sure that Valve is completely closed/opened
    private static final int EXTRA_BUFFER_TIME = 2000;

    private static final int VALUE_BUFFER = 5;

    private static final int MILLI_SECONDS_TO_SECONDS = 1000;
    private static final int MAX_PERCENT_POSSIBLE = 100;
    private static final int MIN_PERCENT_POSSIBLE = 0;

    private Double lastMaximum;
    private Double lastMinimum;
    private Double maximum = 100.d;
    private Double minimum = 0.d;
    private ConfigurationType configurationType;

    @Reference
    ComponentManager cpm;


    public ValveImpl() {
        super(OpenemsComponent.ChannelId.values(), HeatsystemComponent.ChannelId.values());
    }


    @Activate
    void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
        this.getIsBusyChannel().setNextValue(false);
        this.getPowerLevelChannel().setNextValue(0);
        this.getLastPowerLevelChannel().setNextValue(0);
        this.setPointPowerLevelChannel().setNextValue(-1);
        this.futurePowerLevelChannel().setNextValue(0);
        if (config.shouldCloseOnActivation()) {
            this.forceClose();
        }
    }

    /**
     * This will be called on either Activation or Modification.
     *
     * @param config the Config of the Valve
     * @throws ConfigurationException             if anything is configured Wrong
     * @throws OpenemsError.OpenemsNamedException thrown if configured address or Relay cannot be found at all.
     */

    private void activateOrModifiedRoutine(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        this.configurationType = config.configurationType();
        switch (this.configurationType) {
            case CHANNEL:
                this.openAddress = ChannelAddress.fromString(config.open());
                this.closeAddress = ChannelAddress.fromString(config.close());
                if (this.checkChannelOk() == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Channels are not ok!");
                }
                break;
            case DEVICE:
                if (this.checkDevicesOk(config.open(), config.close()) == false) {
                    throw new ConfigurationException("ActivateMethod in Valve: " + super.id(), "Given Devices are not ok!");
                }
                break;
        }
        this.secondsPerPercentage = ((double) config.valve_Time() / 100.d);
        this.timeChannel().setNextValue(0);
    }


    /**
     * Called on Activation or Modification. Checks if the Strings match a Relay configured within OpenEms.
     *
     * @param open  the String/Id of the Relay that opens the Valve
     * @param close the String/Id of the Relay that closes the Valve
     * @return true if the device is ok, otherwise false (Happens if the OpenEmsComponent is not an instance of a Relay)
     * @throws OpenemsError.OpenemsNamedException if the Id is not found at all
     */
    private boolean checkDevicesOk(String open, String close) throws OpenemsError.OpenemsNamedException {
        OpenemsComponent relayToApply = this.cpm.getComponent(open);
        if (relayToApply instanceof Relay) {
            this.openRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        relayToApply = this.cpm.getComponent(close);
        if (relayToApply instanceof Relay) {
            this.closeRelay = (Relay) relayToApply;
        } else {
            return false;
        }
        return true;
    }

    /**
     * Checks if the Channel are correct.
     *
     * @return if the channel are instances of WriteChannel and Type = Boolean
     * @throws OpenemsError.OpenemsNamedException if Channel could not be found
     */
    private boolean checkChannelOk() throws OpenemsError.OpenemsNamedException {
        Channel<?> openChannel = this.cpm.getChannel(this.openAddress);
        Channel<?> closeChannel = this.cpm.getChannel(this.closeAddress);
        return openChannel instanceof WriteChannel<?> && closeChannel instanceof WriteChannel<?> && openChannel.getType()
                .equals(OpenemsType.BOOLEAN) && closeChannel.getType().equals(OpenemsType.BOOLEAN);
    }


    @Modified
    void modified(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.modified(context, config.id(), config.alias(), config.enabled());
        this.activateOrModifiedRoutine(config);
    }


    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            this.lastMaximum = this.maximum;
            this.lastMinimum = this.minimum;
            this.updatePowerLevel();
            this.checkMaxAndMinAllowed();
            boolean reached = this.powerLevelReached() && this.readyToChange();
            if (reached) {
                this.getIsBusyChannel().setNextValue(false);
                this.isForced = false;
                this.adaptValveValue();
            }
        } else if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.shouldReset()) {
                this.reset();
            } else if (this.getForceFullPowerAndResetChannel()) {
                this.forceOpen();
            } else {
                int changeByPercent = this.setPointPowerLevelValue();
                if (changeByPercent >= MIN_PERCENT_POSSIBLE) {
                    changeByPercent -= this.getPowerLevelValue();
                    if (this.changeByPercentage(changeByPercent)) {
                        this.setPointPowerLevelChannel().setNextValue(-1);
                    }
                } else if (this.powerLevelReached() == false) {
                    this.updatePowerLevel();
                }
            }
        }
    }

    /**
     * Check if the Min and/or Max value is defined and valid.
     */
    private void checkMaxAndMinAllowed() {
        Double maxAllowed = this.getMaxAllowedValue();
        Double minAllowed = this.getMinAllowedValue();
        double futurePowerLevel = this.getFuturePowerLevelValue();
        if (maxAllowed != null && maxAllowed + VALUE_BUFFER < futurePowerLevel) {
            this.changeByPercentage(maxAllowed - this.getPowerLevelValue());
        } else if (minAllowed != null && minAllowed - VALUE_BUFFER > futurePowerLevel) {
            this.changeByPercentage(minAllowed - futurePowerLevel);
        }
    }

    /**
     * Only if Reached! this Method will be called.
     */
    private void adaptValveValue() {
        int cycleTime = this.cycle == null ? Cycle.DEFAULT_CYCLE_TIME : this.cycle.getCycleTime();
        double percentPossiblePerCycle = cycleTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
        double limit = percentPossiblePerCycle * 2;
        boolean powerLevelOutOfBounce = this.getPowerLevelValue() - limit > this.getFuturePowerLevelValue() || this.getPowerLevelValue() + limit < this.getFuturePowerLevelValue() || this.getPowerLevelValue() == this.getFuturePowerLevelValue();
        if (percentPossiblePerCycle >= 2 && powerLevelOutOfBounce) {
            try {
                this.setPointPowerLevelChannel().setNextWriteValueFromObject(this.getFuturePowerLevelValue());
            } catch (OpenemsError.OpenemsNamedException e) {
                this.log.warn("Couldn't adapt Valve; Value of Valve: " + super.id());
            }
        }
    }

    //--------------UPDATE POWERLEVEL AND POWER LEVEL REACHED---------------//

    /**
     * Update PowerLevel by getting elapsed Time and check how much time has passed.
     * Current PowerLevel and new Percentage is added together and rounded to 3 decimals.
     */
    private void updatePowerLevel() {
        //Only Update PowerLevel if the Valve is Changing
        if (this.isChanging()) {
            long elapsedTime = this.getMilliSecondTime();
            //If it's the first update of PowerLevel
            if (this.timeStampValveCurrent == -1) {
                //only important for ForceClose/Open
                this.timeStampValveInitial = elapsedTime;
                //First time in change
                elapsedTime = 0;

                //was updated before
            } else {
                elapsedTime -= this.timeStampValveCurrent;
            }
            this.timeStampValveCurrent = this.getMilliSecondTime();
            double percentIncrease = elapsedTime / (this.secondsPerPercentage * MILLI_SECONDS_TO_SECONDS);
            if (this.isClosing) {
                percentIncrease *= -1;
            }
            //Round the calculated PercentIncrease of current PowerLevel and percentIncrease to 3 decimals
            Double powerLevel = this.getPowerLevelValue();
            double truncatedDouble = powerLevel == null ? 0 : BigDecimal.valueOf(powerLevel + percentIncrease)
                    .setScale(3, RoundingMode.HALF_UP)
                    .doubleValue();
            if (truncatedDouble > MAX_PERCENT_POSSIBLE) {
                truncatedDouble = MAX_PERCENT_POSSIBLE;
            } else if (truncatedDouble < MIN_PERCENT_POSSIBLE) {
                truncatedDouble = MIN_PERCENT_POSSIBLE;
            }
            this.getPowerLevelChannel().setNextValue(truncatedDouble);
        } else {
            this.timeStampValveCurrent = -1;
        }
    }

    /**
     * Check if Valve has reached the set-point and shuts down Relays if true. (No further opening and closing of Valve)
     *
     * @return is powerLevelReached
     */
    @Override
    public boolean powerLevelReached() {
        boolean reached = true;
        if (this.isChanging()) {
            reached = false;
            Double powerLevel = this.getPowerLevelValue();
            Double futurePowerLevel = this.getFuturePowerLevelValue();
            if (powerLevel != null && futurePowerLevel != null) {
                if (this.isClosing) {
                    reached = powerLevel <= futurePowerLevel;
                } else {
                    reached = powerLevel >= futurePowerLevel;
                }
            }
        }
        //ReadyToChange always True except if it is forced
        reached = reached && this.readyToChange();
        if (reached) {
            this.isChanging = false;
            this.timeStampValveCurrent = -1;
            this.shutdownRelays();
        }
        return reached;
    }

    // ------------------------------------------------------------- //

    // --------------- READY TO CHANGE AND CHANGE BY PERCENTAGE ------------ //

    /**
     * Ready To Change is always true except if the Valve was forced to open/close and the Time to close/open the
     * Valve completely is not over.
     */
    @Override
    public boolean readyToChange() {
        if (this.isForced) {
            long currentTime = this.getMilliSecondTime();
            if (currentTime - this.timeStampValveInitial
                    >= ((this.timeNeeded() * 1000) + EXTRA_BUFFER_TIME)) {
                this.getIsBusyChannel().setNextValue(false);
                this.wasAlreadyReset = false;
                this.isForced = false;
                return true;
            } else {
                return false;
            }
        }
        return true;
    }


    /**
     * Changes Valve Position by incoming percentage.
     * Warning, only executes if valve is not busy! (was not forced to open/close)
     * Depending on + or - it changes the current State to open/close it more. Switching the relays on/off does
     * not open/close the valve instantly but slowly. The time it takes from completely closed to completely
     * open is entered in the config. Partial open state of x% is then archived by switching the relay on for
     * time-to-open * x%, or the appropriate amount of time depending on initial state.
     * Sets the Future PowerLevel; ValveManager calls further Methods to refresh true % state
     *
     * @param percentage adjusting the current powerLevel in % points. Meaning if current state is 10%, requesting
     *                   changeByPercentage(20) will change the state to 30%.
     *                   <p>
     *                   If the Valve is busy return false
     *                   otherwise: save the current PowerLevel to the old one and overwrite the new one.
     *                   Then it will check how much time is needed to adjust the position of the valve.
     *                   If percentage is neg. valve needs to be closed (further)
     *                   else it needs to open (further).
     *                   </p>
     */
    @Override
    public boolean changeByPercentage(double percentage) {


        if (this.readyToChange() == false || percentage == MIN_PERCENT_POSSIBLE) {
            return false;
        } else {
            Double currentPowerLevel = this.getPowerLevelValue();
            //Setting the oldPowerLevel and adjust the percentage Value
            this.getLastPowerLevelChannel().setNextValue(currentPowerLevel);
            this.maximum = getMaxAllowedValue();
            this.minimum = getMinAllowedValue();
            if (this.maxMinValid() == false) {
                this.minimum = null;
                this.maximum = null;
            }
            currentPowerLevel += percentage;
            if (this.maximum != null && this.maximum < currentPowerLevel) {
                currentPowerLevel = this.maximum;
            } else if (this.lastMaximum != null && this.lastMaximum < currentPowerLevel) {
                currentPowerLevel = this.lastMaximum;
            } else if (currentPowerLevel >= MAX_PERCENT_POSSIBLE) {
                currentPowerLevel = (double) MAX_PERCENT_POSSIBLE;
            } else if (this.minimum != null && this.minimum > currentPowerLevel) {
                currentPowerLevel = this.minimum;
            } else if (this.lastMinimum != null && this.lastMinimum > currentPowerLevel) {
                currentPowerLevel = this.lastMinimum;
            }
            //Set goal Percentage for future reference
            this.futurePowerLevelChannel().setNextValue(currentPowerLevel);
            //if same power level do not change and return --> relays is not always powered
            Double lastPower = this.getLastPowerLevelValue();
            if (lastPower.equals(currentPowerLevel)) {
                this.isChanging = false;
                this.shutdownRelays();
                return false;
            }
            //Calculate the Time to Change the Valve
            if (Math.abs(percentage) >= MAX_PERCENT_POSSIBLE) {
                this.timeChannel().setNextValue(MAX_PERCENT_POSSIBLE * this.secondsPerPercentage);
            } else {
                this.timeChannel().setNextValue(Math.abs(percentage) * this.secondsPerPercentage);
            }
            //Close on negative Percentage and Open on Positive
            this.isChanging = true;
            if (percentage < MIN_PERCENT_POSSIBLE) {
                this.valveClose();
            } else {
                this.valveOpen();
            }
            return true;
        }
    }
    //------------------------------------------------------ //

    /**
     * IS Changing --> Is closing/Opening.
     *
     * @return isChanging
     */
    @Override
    public boolean isChanging() {
        return this.isChanging;
    }

    //---------------------RESET------------------------- //

    /**
     * Resets the Valve and forces to close.
     * Was Already Reset prevents multiple forceCloses if Channel not refreshed in time.
     */
    @Override
    public void reset() {
        if (this.wasAlreadyReset == false) {
            this.forceClose();
            this.wasAlreadyReset = true;
        }

    }

    /**
     * Checks if the Valve should be reset.
     *
     * @return shouldReset.
     */
    private boolean shouldReset() {
        if (this.wasAlreadyReset) {
            return false;
        } else {
            return this.getResetValueAndResetChannel();
        }
    }


    // ------------ FORCE OPEN AND CLOSE------------------ //

    /**
     * Closes the valve completely, overriding any current valve operation.
     * If a closed valve is all you need, better use this instead of changeByPercentage(-100) as you do not need
     * to check if the valve is busy or not.
     * Usually called to Reset a Valve or ForceClose the Valve on an Error.
     */
    @Override
    public void forceClose() {
        if (this.isForced == false || this.isClosing == false) {
            this.isForced = true;
            this.isChanging = true;
            this.futurePowerLevelChannel().setNextValue(MIN_PERCENT_POSSIBLE);
            this.timeChannel().setNextValue(MAX_PERCENT_POSSIBLE * this.secondsPerPercentage);
            this.valveClose();
            this.getIsBusyChannel().setNextValue(true);
            //Making sure to wait the correct time even if it is already closing.
            this.timeStampValveInitial = -1;
            this.updatePowerLevel();

        }

    }

    /**
     * Opens the valve completely, overriding any current valve operation.
     * If an open valve is all you need, better use this instead of changeByPercentage(100) as you do not need
     * to check if the valve is busy or not.
     */
    @Override
    public void forceOpen() {
        if (this.isForced == false || this.isClosing == true) {
            this.isForced = true;
            this.isChanging = true;
            this.futurePowerLevelChannel().setNextValue(MAX_PERCENT_POSSIBLE);
            this.timeChannel().setNextValue(MAX_PERCENT_POSSIBLE * this.secondsPerPercentage);
            this.valveOpen();
            this.getIsBusyChannel().setNextValue(true);
            //Making sure to wait the correct time even if it is already opening
            this.timeStampValveInitial = -1;
            this.updatePowerLevel();
        }

    }

    //-------------------------------------------------------------//


    //---------------------ShutDown Relay-------------------------//

    /**
     * Turn off Relay if PowerLevel is reached.
     */
    private void shutdownRelays() {
        this.controlRelays(false, "Open");
        this.controlRelays(false, "Closed");
    }

    // -------------------------------------- //


    // ---------- CLOSE AND OPEN VALVE ------------ //

    /**
     * Closes the valve and sets a time stamp.
     */
    private void valveClose() {

        this.controlRelays(false, "Open");
        this.controlRelays(true, "Closed");
        if (this.isClosing == false) {
            this.timeStampValveCurrent = -1;
            this.isClosing = true;
        }

    }

    /**
     * Opens the valve and sets a time stamp.
     * DO NOT CALL DIRECTLY! Might not work if called directly as the timer for "readyToChange()" is not
     * set properly. Use either "changeByPercentage()" or forceClose / forceOpen.
     */
    private void valveOpen() {

        this.controlRelays(false, "Closed");
        this.controlRelays(true, "Open");
        if (this.isClosing == true) {
            this.timeStampValveCurrent = -1;
            this.isClosing = false;
        }
    }
    //-------------------------------------


    /**
     * Controls the relays by typing either activate or not and what relays should be called.
     * If ExceptionHandling --> use forceClose or forceOpen!
     *
     * @param activateOrDeactivate activate or deactivate.
     * @param whichRelays          opening or closing relays ?
     *                             <p>Writes depending if the relays is an opener or closer, the correct boolean.
     *                             if the relays was set false (no power) busy will be false.</p>
     */
    private void controlRelays(boolean activateOrDeactivate, String whichRelays) {
        try {
            switch (whichRelays) {
                case "Open":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> openChannel = this.cpm.getChannel(this.openAddress);
                            openChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.openRelay.getRelaysWriteChannel().setNextWriteValueFromObject(activateOrDeactivate);
                    }
                    break;

                case "Closed":
                    if (this.configurationType.equals(ConfigurationType.CHANNEL)) {
                        if (this.checkChannelOk()) {
                            WriteChannel<Boolean> closeChannel = this.cpm.getChannel(this.closeAddress);
                            closeChannel.setNextWriteValue(activateOrDeactivate);
                        }
                    } else {
                        this.closeRelay.setRelayStatus(activateOrDeactivate);
                    }
                    break;

            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't write into Channel; Valve: " + super.id());
        }
    }


    // --------- UTILITY -------------//

    /**
     * Get Current Time in Ms.
     *
     * @return currentTime in Ms.
     */

    private long getMilliSecondTime() {
        long time = System.nanoTime();
        return TimeUnit.MILLISECONDS.convert(time, TimeUnit.NANOSECONDS);
    }


    /**
     * Checks if the max and Min Values are correct.
     *
     * @return validation.
     */

    private boolean maxMinValid() {
        if (this.maximum == null) {
            this.maximum = (double) MAX_PERCENT_POSSIBLE;
        }
        if (this.minimum == null) {
            this.minimum = (double) MIN_PERCENT_POSSIBLE;
        }
        return (this.maximum >= this.minimum && this.maximum > MIN_PERCENT_POSSIBLE && this.minimum >= MIN_PERCENT_POSSIBLE);
    }


    // ----------------------------- //


    @Override
    public String debugLog() {
        if (this.getPowerLevelChannel().value().isDefined()) {
            String name = "";
            if (!super.alias().equals("")) {
                name = super.alias();
            } else {
                name = super.id();
            }
            return "Valve: " + name + ": " + this.getPowerLevelValue() + "\n";
        } else {
            return "\n";
        }
    }


    @Deactivate
    protected void deactivate() {
        super.deactivate();
    }
}

