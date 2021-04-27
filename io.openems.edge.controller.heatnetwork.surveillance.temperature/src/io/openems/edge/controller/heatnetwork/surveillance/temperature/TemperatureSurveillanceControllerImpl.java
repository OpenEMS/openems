package io.openems.edge.controller.heatnetwork.surveillance.temperature;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.surveillance.temperature.api.TemperatureSurveillanceController;
import io.openems.edge.controller.heatnetwork.valve.api.ControlType;
import io.openems.edge.controller.heatnetwork.valve.api.ValveController;
import io.openems.edge.heater.Heater;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerThreshold;
import org.joda.time.DateTime;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Designate(ocd = Config.class, factory = true)
@Component(name = "controller.temperature.surveillance",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class TemperatureSurveillanceControllerImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, TemperatureSurveillanceController {

    private final Logger log = LoggerFactory.getLogger(TemperatureSurveillanceControllerImpl.class);

    @Reference
    ComponentManager cpm;
    //Different ThresholdThermometer
    private Thermometer referenceThermometer;
    private Thermometer activationThermometer;
    private Thermometer deactivationThermometer;
    private int activationOffset;
    private int deactivationOffset;
    private ValveController optionalValveController;
    private Heater optionalHeater;
    private TemperatureSurveillanceType surveillanceType;
    private DateTime initialWaitTimeStamp;
    private int waitTimeInSeconds;
    private boolean hadToWaitBefore;
    private boolean isRunning;

    public TemperatureSurveillanceControllerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (config.thermometerActivateId().equals(config.thermometerDeactivateId()) || config.thermometerActivateId().equals(config.referenceThermometerId())) {
            throw new ConfigurationException("Activate - TemperatureSurveillanceController",
                    "Activate and Deactivate and Reference Thermometer are not allowed to be the same! "
                            + config.thermometerActivateId() + config.thermometerDeactivateId() + config.referenceThermometerId());
        }
        allocateComponents(config);
        //other config setup
        this.activationOffset = config.offsetActivate();
        this.deactivationOffset = config.offsetDeactivate();
        this.waitTimeInSeconds = config.timeToWait();
        if (config.useHeater() && config.useValveController()) {
            this.surveillanceType = TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER;
        } else if (config.useValveController()) {
            this.surveillanceType = TemperatureSurveillanceType.VALVE_CONTROLLER_ONLY;
        } else if (config.useHeater()) {
            this.surveillanceType = TemperatureSurveillanceType.HEATER_ONLY;
        } else {
            this.surveillanceType = TemperatureSurveillanceType.NOTHING;
        }


    }

    private void allocateComponents(Config config) throws ConfigurationException, OpenemsError.OpenemsNamedException {
        OpenemsComponent openemsComponentToAllocate;
        openemsComponentToAllocate = cpm.getComponent(config.referenceThermometerId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.referenceThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThermometerId: "
                    + config.referenceThermometerId() + " Not an Instance of Thermometer");
        }


        openemsComponentToAllocate = cpm.getComponent(config.thermometerActivateId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.activationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + config.thermometerActivateId() + " Not an Instance of ThresholdThermometer");
        }
        openemsComponentToAllocate = cpm.getComponent(config.thermometerDeactivateId());
        if (openemsComponentToAllocate instanceof Thermometer) {
            this.deactivationThermometer = (Thermometer) openemsComponentToAllocate;
        } else {
            throw new ConfigurationException("AllocateComponents", "ThresholdThermometerId: "
                    + config.thermometerDeactivateId() + " Not an Instance of ThresholdThermometer");
        }
        if (config.useHeater()) {
            openemsComponentToAllocate = cpm.getComponent(config.heaterId());
            if (openemsComponentToAllocate instanceof Heater) {
                this.optionalHeater = (Heater) openemsComponentToAllocate;
            } else {
                throw new ConfigurationException("AllocateComponents", "HeaterId: "
                        + config.heaterId() + " Not an Instance of Heater");
            }
        }
        if (config.useValveController()) {
            openemsComponentToAllocate = cpm.getComponent(config.valveControllerId());
            if (openemsComponentToAllocate instanceof ValveController) {
                this.optionalValveController = (ValveController) openemsComponentToAllocate;
            } else {
                throw new ConfigurationException("AllocateComponents", "ValveControllerId: "
                        + config.valveControllerId() + " Not an Instance of ValveController");
            }
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    /**
     * Depending on SurveillanceType Different "Controlling" applies
     * If AcitvationConditions apply (Same for every SurveillanceType).
     * <p>
     * Either: Enable HEATER (on Heater ONLY mode)
     * OR
     * Enable ValveController (on ValveController ONLY Mode
     * OR
     * First Enable Heater and then after certain WaitTime Enable ValveController (HEATER_AND_VALVE_CONTROLLER.
     * on deactivation: Disable corresponding Components
     * </p>
     *
     * @throws OpenemsError.OpenemsNamedException if controller couldn't write next Value of Heater enableSignalChannel.
     */
    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        try {
            checkForMissingComponents();
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.warn("Couldn't reallocate All components: " + e.getMessage());
        }
        if (activationConditionsApply() || isRunning) {
            isRunning = true;
            switch (this.surveillanceType) {
                case HEATER_ONLY:
                    this.optionalHeater.getEnableSignalChannel().setNextWriteValue(true);
                    break;
                case VALVE_CONTROLLER_ONLY:
                    this.optionalValveController.setEnableSignal(true);
                    this.optionalValveController.setControlType(ControlType.TEMPERATURE);
                    break;
                case HEATER_AND_VALVE_CONTROLLER:
                    this.optionalHeater.getEnableSignalChannel().setNextWriteValue(true);
                    if (checkIsTimeUp()) {
                        this.hadToWaitBefore = false;
                        this.optionalValveController.setEnableSignal(true);
                        this.optionalValveController.setControlType(ControlType.TEMPERATURE);
                    } else {
                        this.optionalValveController.setEnableSignal(false);
                    }
                    break;
                case NOTHING:
                    break;
            }
        } else if (deactivationConditionsApply()) {
            isRunning = false;
        }
    }

    /**
     * Reallocates the Component if one Component was reactivated.
     *
     * @throws OpenemsError.OpenemsNamedException if Component is Missing.
     */
    private void checkForMissingComponents() throws OpenemsError.OpenemsNamedException {
        OpenemsComponent allocatedOpenemsComponent;
        if (this.activationThermometer.isEnabled() == false) {
            allocatedOpenemsComponent = cpm.getComponent(this.activationThermometer.id());
            if (allocatedOpenemsComponent instanceof ThermometerThreshold) {
                this.activationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
            }
        }
        if (this.deactivationThermometer.isEnabled() == false) {
            allocatedOpenemsComponent = cpm.getComponent(this.deactivationThermometer.id());
            if (allocatedOpenemsComponent instanceof ThermometerThreshold) {
                this.deactivationThermometer = (ThermometerThreshold) allocatedOpenemsComponent;
            }
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.NOTHING)) {
            return;
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_ONLY)
                || this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)) {
            if (this.optionalHeater.isEnabled() == false) {
                allocatedOpenemsComponent = cpm.getComponent(this.optionalHeater.id());
                if (allocatedOpenemsComponent instanceof Heater) {
                    this.optionalHeater = (Heater) allocatedOpenemsComponent;
                }

            }
        }
        if (this.surveillanceType.equals(TemperatureSurveillanceType.VALVE_CONTROLLER_ONLY)
                || this.surveillanceType.equals(TemperatureSurveillanceType.HEATER_AND_VALVE_CONTROLLER)) {
            if (this.optionalValveController.isEnabled() == false) {
                allocatedOpenemsComponent = cpm.getComponent(this.optionalValveController.id());
                if (allocatedOpenemsComponent instanceof ValveController) {
                    this.optionalValveController = (ValveController) allocatedOpenemsComponent;
                }
            }
        }
    }

    /**
     * Check if Time To Wait is up by creating initialTimeStamp first; return false
     * And after that, check if TimeStamp + Waittime is after current Time.
     *
     * @return the TimeStampCheck
     */
    private boolean checkIsTimeUp() {
        if (hadToWaitBefore == false) {
            this.hadToWaitBefore = true;
            this.initialWaitTimeStamp = DateTime.now();
            return false;
        } else {
            DateTime now = DateTime.now();
            DateTime compare = new DateTime(this.initialWaitTimeStamp);
            compare = compare.plusSeconds(this.waitTimeInSeconds);
            return now.isAfter(compare);
        }

    }

    /**
     * The Deactivation Condition is: DeactivationThermometer > SetPoint.
     *
     * @return the comparison boolean.
     */
    private boolean deactivationConditionsApply() {
        return this.referenceThermometer.getTemperatureValue() > (this.deactivationThermometer.getTemperatureValue() + this.deactivationOffset);
    }

    /**
     * The ActivationCondition is ActivationThermometer < setPoint+Offset.
     *
     * @return the comparison boolean.
     */
    private boolean activationConditionsApply() {
        return this.referenceThermometer.getTemperatureValue() < this.activationThermometer.getTemperatureValue() + this.activationOffset;
    }
}
