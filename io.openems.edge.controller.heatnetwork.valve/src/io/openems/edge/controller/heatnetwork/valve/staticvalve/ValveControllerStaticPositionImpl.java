package io.openems.edge.controller.heatnetwork.valve.staticvalve;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.valve.api.ControlType;
import io.openems.edge.controller.heatnetwork.valve.api.ValveController;
import io.openems.edge.controller.heatnetwork.valve.api.ValveControllerStaticPosition;
import io.openems.edge.controller.heatnetwork.valve.api.ValvePosition;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.thermometer.api.Thermometer;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Valve.StaticPosition", immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ValveControllerStaticPositionImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ValveControllerStaticPosition {

    private static final int ENTRY_LENGTH = 2;
    @Reference
    ComponentManager cpm;

    private Valve controlledValve;
    private final List<ValvePosition> valvePositionList = new ArrayList<>();
    private ControlType controlType;
    private boolean closeWhenNeitherAutoRunNorEnableSignal;
    //Can be ANY Thermometer! (VirutalThermometer would be the best)
    private Thermometer referenceThermometer;
    private boolean isFallback;
    //Wait this amount of cycles if no EnabledSignal is present!
    private static final int MAX_WAIT_CYCLES = 10;
    //if enabled signal stays null this component runs for this amount of time:
    private static final int MIN_RUN_TIME_AFTER_MAX_WAIT_CYLCES_SECONDS = 60;
    private final AtomicInteger currentCycles = new AtomicInteger(0);

    private DateTime initialTimeStamp;
    private boolean hadToFallbackBefore;

    public ValveControllerStaticPositionImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values(),
                ValveController.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {

        super.activate(context, config.id(), config.alias(), config.enabled());
        if (this.cpm.getComponent(config.valveToControl()) instanceof Valve) {
            this.controlledValve = cpm.getComponent(config.valveToControl());
        } else {
            throw new ConfigurationException("ActivateMethod StaticValveController", config.valveToControl() + " Not an instance of Valve");
        }
        ControlType controlTypeOfThisRun = this.getControlType();
        if (controlTypeOfThisRun != null) {
            this.controlType = controlTypeOfThisRun;
        }

        OpenemsComponent componentToFetch = this.cpm.getComponent(config.thermometerId());
        if (componentToFetch instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) componentToFetch;
        } else {
            throw new ConfigurationException("Activate of ValveControllerStaticPosition", "Instance of "
                    + config.thermometerId() + " is not a Thermometer");
        }
        ConfigurationException[] exceptions = {null};
        //Split entry: temperature:ValueOfValve
        Arrays.asList(config.temperaturePositionMap()).forEach(entry -> {
            if (exceptions[0] == null && entry.contains(":") && entry.equals("") == false) {
                try {
                    String[] entries = entry.split(":");
                    if (entries.length != ENTRY_LENGTH) {
                        throw new ConfigurationException("activate StaticValveController", "Entries: " + entries.length + " expected : " + ENTRY_LENGTH);
                    }
                    int temperature = Integer.parseInt(entries[0]);
                    double valvePosition = Double.parseDouble(entries[1]);
                    this.valvePositionList.add(new ValvePosition(temperature, valvePosition));
                } catch (ConfigurationException e) {
                    exceptions[0] = e;
                }
            }
        });
        this.valvePositionList.add(new ValvePosition(1000, config.defaultPosition()));
        if (exceptions[0] != null) {
            throw exceptions[0];
        }
        if (ControlType.contains(config.controlType().toUpperCase().trim())) {
            this.controlType = ControlType.valueOf(config.controlType().toUpperCase().trim());
        } else {
            throw new ConfigurationException("ControlTypeConfig", config.controlType() + " does not exist");
        }
        this.setAutoRun(config.autorun());
        this.closeWhenNeitherAutoRunNorEnableSignal = config.shouldCloseWhenNoSignal();
        this.forceAllowedChannel().setNextValue(config.allowForcing());
        this.isFallback = config.useFallback();
    }

    private void setPositionByTemperature(int temperature) {
        if ((temperature == Integer.MIN_VALUE) == false) {
            AtomicReference<ValvePosition> selectedPosition = new AtomicReference<>();
            selectedPosition.set(this.valvePositionList.get(0));
            this.valvePositionList.forEach(valvePosition -> {
                //As long as position Temperature < current Temp && position temperature greater than current Position temp.
                // e.g. Temperature is 50; current position in iteration is 45 and selected position temp was 42
                if (valvePosition.getTemperature() < temperature && valvePosition.getTemperature() > selectedPosition.get().getTemperature()) {
                    selectedPosition.set(valvePosition);
                    //if current Position is greater Than temp -> check for either : selected Pos beneath temp -> select current position
                    // OR if current pos has lower temp but selected is greater than current --> select current
                    //Example: Temperature 50; selected position 45; new has 55; take 55
                    //new iteration temperature 50; selected 55; current is 52; take 52 position
                } else if (valvePosition.getTemperature() >= temperature) {
                    if (valvePosition.getTemperature() < selectedPosition.get().getTemperature()
                            && (selectedPosition.get().getTemperature() < temperature || valvePosition.getTemperature() < selectedPosition.get().getTemperature())) {
                        selectedPosition.set(valvePosition);
                    }
                }
            });
            this.controlledValve.setPowerLevelPercent().setNextValue(selectedPosition.get().getValvePosition());
            this.setSetPointPosition((int) selectedPosition.get().getValvePosition());
        }

    }

    /**
     * Sets Position by concrete Percentage Value, written into the requested PositionChannel.
     *
     * @param percent the percent of requested Position.
     */
    private void setPositionByPercent(int percent) {
        if (this.controlledValve.readyToChange() && percent != Integer.MIN_VALUE) {
            this.controlledValve.setPowerLevelPercent().setNextValue(percent);
            this.setSetPointPosition(percent);
            if (this.controlledValve.powerLevelReached()) {
                try {
                    this.getRequestedPositionChannel().setNextWriteValue(null);
                } catch (OpenemsError.OpenemsNamedException ignored) {
                }
            }
        }
    }

    /**
     * IF either autoRun or enable signal --> Check current Temperature (set by other component).
     * check for Forcing and if forceClose or ForceOpen is force the Valve
     * Otherwise: check for ControlType; Get the needed variable (percent or temperature)
     * and control Valve by variables (See setPositionBy Percent or setPositionByTemperature for more info)
     */
    @Override
    public void run() {
        //Check Requested Position
        checkComponentsStillEnabled();
        //TODO Do getNextWriteValueAndReset!
        if (this.isEnabledOrAutoRun()) {
            //check for forceOpen/Close
            if (forceAllowed()) {
                if (isForcedOpen()) {
                    this.forceOpenValve();
                } else if (isForcedClose()) {
                    this.forceCloseValve();
                }
            } else {
                switch (this.controlType) {
                    case POSITION:
                        int percent = this.getRequestedValvePosition();
                        this.setPositionByPercent(percent);
                        break;
                    case TEMPERATURE:
                        int temperature = this.referenceThermometer.getTemperatureValue();
                        this.setPositionByTemperature(temperature);
                        break;
                }
            }
        } else {
            if (this.closeWhenNeitherAutoRunNorEnableSignal) {
                if (this.controlledValve.getCurrentPowerLevelValue() != 0) {
                    this.controlledValve.setPowerLevelPercent().setNextValue(0);
                }
            }
        }
    }

    /**
     * Checks if the Controller is allowed to Run (Either if it's autorun, the EnabledSignal is
     * Present AND true OR EnabledSignal is Missing AND fallback is active).
     *
     * @return enabled;
     */
    private boolean isEnabledOrAutoRun() {

        if (this.isAutorun()) {
            return true;
        } else {
            Optional<Boolean> enableSignal = this.getEnableSignalChannel().getNextWriteValueAndReset();
            if (enableSignal.isPresent()) {
                this.hadToFallbackBefore = false;
                this.currentCycles.set(0);
                return enableSignal.get();
            } else {
                //Fallback
                if (this.isFallback) {
                    return fallbackActivation();
                } else {
                    return false;
                }
            }
        }
    }

    /**
     * .
     * If Fallback is enabled ->check if WaitCycles are at max is up and if that's the case check if Time is up
     * if: currentCycle > Max --> Check if Time is up (Controller did run for a min) --> if Time up --> Return false and reset
     * else: return true
     *
     * @return boolean: fallbackResult.
     */

    private boolean fallbackActivation() {
        //First check Cycles
        if (this.currentCycles.get() > MAX_WAIT_CYCLES) {
            //check TimeWasSet?
            if (this.hadToFallbackBefore == false) {
                this.initialTimeStamp = new DateTime();
                this.hadToFallbackBefore = true;
                return true;
            } else {
                //check isTimeUp?
                DateTime now = new DateTime();
                DateTime then = new DateTime(this.initialTimeStamp);
                if (now.isAfter(then.plusSeconds(MIN_RUN_TIME_AFTER_MAX_WAIT_CYLCES_SECONDS))) {
                    this.hadToFallbackBefore = false;
                    this.currentCycles.set(0);
                    return false;
                } else {
                    return true;
                }
            }
        } else {
            this.currentCycles.getAndIncrement();
            return false;
        }
    }

    private void forceCloseValve() {
        if (this.controlledValve.readyToChange()) {
            this.controlledValve.forceClose();
            this.isForcedCloseChannel().setNextValue(false);
        }
    }

    private void forceOpenValve() {
        if (this.controlledValve.readyToChange()) {
            this.controlledValve.forceOpen();
            this.isForcedOpenChannel().setNextValue(false);
        }
    }

    private void checkComponentsStillEnabled() {
        try {
            if (this.controlledValve.isEnabled() == false) {
                if (this.cpm.getComponent(this.controlledValve.id()) instanceof Valve) {
                    this.controlledValve = this.cpm.getComponent(this.controlledValve.id());
                }
            }
            if (this.referenceThermometer.isEnabled() == false) {
                if (this.cpm.getComponent(this.referenceThermometer.id()) instanceof Thermometer) {
                    this.referenceThermometer = this.cpm.getComponent(this.referenceThermometer.id());
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
        //@Paul soll hier noch was passieren?
    }

    @Override
    public double getPositionByTemperature(int temperature) {
        //TODO not important atm
        return 0;
    }

    @Override
    public int getTemperatureByPosition(double position) {
        //TODO not important atm
        return 0;
    }

    @Override
    public void addPositionByTemperatureAndPosition(int temperature, int valvePosition) {
        Optional<ValvePosition> containingPosition = this.valvePositionList.stream().filter(position -> position.getTemperature() == temperature).findFirst();
        if (containingPosition.isPresent()) {
            containingPosition.get().setValvePosition(valvePosition);
        } else {
            this.valvePositionList.add(new ValvePosition(temperature, valvePosition));
        }
    }

    @Override
    public double getCurrentPositionOfValve() {
        return this.controlledValve.getCurrentPowerLevelValue();
    }
}
