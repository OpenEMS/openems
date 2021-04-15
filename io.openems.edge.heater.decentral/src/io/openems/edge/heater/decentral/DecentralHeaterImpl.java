package io.openems.edge.heater.decentral;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.valve.api.ControlType;
import io.openems.edge.controller.valve.api.ValveController;
import io.openems.edge.heater.decentral.api.DecentralHeater;
import io.openems.edge.heater.api.Heater;
import io.openems.edge.heater.api.HeaterState;
import io.openems.edge.heatsystem.components.Valve;
import io.openems.edge.thermometer.api.ThresholdThermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;


@Designate(ocd = Config.class, factory = true)
@Component(name = "Heater.Decentral",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS}
)

public class DecentralHeaterImpl extends AbstractOpenemsComponent implements OpenemsComponent, DecentralHeater, EventHandler {

    @Reference
    ComponentManager cpm;

    private Valve configuredValve;
    private ValveController configuredValveController;
    private boolean isValve;
    private ThresholdThermometer thresholdThermometer;
    private final AtomicInteger currentWaitCycleNeedHeatEnable = new AtomicInteger(0);
    private int maxWaitCyclesNeedHeatEnable;
    private boolean wasNeedHeatEnableLastCycle;

    public DecentralHeaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Heater.ChannelId.values(),
                DecentralHeater.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        OpenemsComponent componentFetchedByComponentManager;

        this.isValve = config.valveOrController().equals("Valve");
        componentFetchedByComponentManager = this.cpm.getComponent(config.valveOrControllerId());
        if (isValve) {
            if (componentFetchedByComponentManager instanceof Valve) {
                this.configuredValve = (Valve) componentFetchedByComponentManager;
            } else {
                throw new ConfigurationException("activate", "The Component with id: "
                        + config.valveOrControllerId() + " is not a Valve");
            }
        } else if (componentFetchedByComponentManager instanceof ValveController) {
            this.configuredValveController = (ValveController) componentFetchedByComponentManager;
        } else {
            throw new ConfigurationException("activate", "The Component with id "
                    + config.valveOrControllerId() + "not an instance of ValveController");
        }

        componentFetchedByComponentManager = cpm.getComponent(config.thresholdThermometerId());
        if (componentFetchedByComponentManager instanceof ThresholdThermometer) {
            this.thresholdThermometer = (ThresholdThermometer) componentFetchedByComponentManager;
            this.thresholdThermometer.setSetPointTemperature(config.setPointTemperature(), super.id());
        } else {
            throw new ConfigurationException("activate",
                    "Component with ID: " + config.thresholdThermometerId() + " not an instance of Threshold");
        }
        this.setSetPointTemperature(config.setPointTemperature());
        if (config.shouldCloseOnActivation()) {
            if (isValve) {
                this.configuredValve.forceClose();
            } else {
                this.configuredValveController.setEnableSignal(false);
            }
        }
        this.getForceHeatChannel().setNextValue(config.forceHeating());
        this.maxWaitCyclesNeedHeatEnable = config.waitCyclesNeedHeatResponse();
        this.setState(HeaterState.OFFLINE.name());
    }


    @Override
    public void setOffline() throws OpenemsError.OpenemsNamedException {
        this.getEnableSignalChannel().setNextWriteValue(false);
    }

    /**
     * The Logic of the Heater.
     * --> Should heat? --> enable Signal of Heater
     * --> Request NeedHeat
     * --> Wait till response (OR ForceHeat @Paul da bin ich mir nicht sicher...)
     * --> check if heat is ok --> else request more heat
     * --> check if valve or valveController
     * --> if valve-->open 100% if  heat ok
     * --> else request in valveController --> position by temperature value
     * --> if shouldn't heat --> call deactivateControlledComponents (requests to false, threshold release Id, etc)
     *
     * @param event The Event of OpenemsEdge.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS)) {
            if (this.errorInHeater()) {
                //TODO DO SOMETHING (?)
            }
            checkMissingComponents();
            //First things first: Is Heater Enabled
            boolean currentRunHeaterEnabled = checkIsCurrentRunHeaterEnabled();
            if (currentRunHeaterEnabled) {
                this.getNeedHeatChannel().setNextValue(true);
                //Is Heater allowed to Heat
                boolean currentRunNeedHeatEnable = checkIsCurrentHeatNeedEnabled();
                if (currentRunNeedHeatEnable || this.getIsForceHeating()) {
                    this.currentWaitCycleNeedHeatEnable.getAndSet(0);
                    this.wasNeedHeatEnableLastCycle = true;
                    //activateThresholdThermometer and check if setPointTemperature can be met otherwise shut valve
                    // and ask for more heat
                    this.setThresholdAndControlValve();
                } else {
                    this.wasNeedHeatEnableLastCycle = false;
                    this.setState(HeaterState.AWAIT.name());
                    this.closeValveOrDisableValveController();
                }
            } else {
                deactivateControlledComponents();
            }
        }
    }

    /**
     * If Controller is Enabled AND permission to heat is set.
     * Check if ThresholdThermometer is ok --> if yes activate Valve/ValveController --> Else Close Valve and say "I need more Heat".
     */
    private void setThresholdAndControlValve() {
        this.thresholdThermometer.setSetPointTemperatureAndActivate(this.getSetPointTemperature(), super.id());
        //Static Valve Controller Works on it's own with given Temperature
        if (this.isValve == false) {
            this.configuredValveController.setEnableSignal(true);
            this.configuredValveController.setControlType(ControlType.TEMPERATURE);
        }
        // Check if SetPointTemperature above Thermometer --> Either
        if (this.thresholdThermometer.thermometerAboveGivenTemperature(this.getSetPointTemperature())) {
            this.setState(HeaterState.RUNNING.name());
            this.getNeedMoreHeatChannel().setNextValue(false);
            if (this.isValve) {
                this.configuredValve.setPowerLevelPercent().setNextValue(100);
            }
        } else {
            this.getNeedMoreHeatChannel().setNextValue(true);
            if (this.isValve) {
                this.closeValveOrDisableValveController();
            }
            this.setState(HeaterState.PREHEAT.name());
        }
    }

    /**
     * This methods checks if the enabled Signal for need Heat was set OR if the Signal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles >= Max Wait. If in doubt --> HEAT
     *
     * @return enabled;
     */
    private boolean checkIsCurrentHeatNeedEnabled() {
        boolean currentRunNeedHeatEnable = this.currentWaitCycleNeedHeatEnable.get() >= this.maxWaitCyclesNeedHeatEnable || wasNeedHeatEnableLastCycle;

        Optional<Boolean> needHeatEnableSignal = this.getNeedHeatEnableSignalChannel().getNextWriteValueAndReset();
        if (needHeatEnableSignal.isPresent()) {
            this.currentWaitCycleNeedHeatEnable.set(0);
            currentRunNeedHeatEnable = needHeatEnableSignal.get();
        } else if (this.currentWaitCycleNeedHeatEnable.get() < this.maxWaitCyclesNeedHeatEnable) {
            this.currentWaitCycleNeedHeatEnable.getAndIncrement();
        }
        return currentRunNeedHeatEnable;
    }

    /**
     * This methods checks if the enabled Signal was set OR if the enableSignal isn't Present -->
     * check if last Cycle was enabled and currentWaitCycles > Max Wait.
     * if in doubt --> HEAT!
     *
     * @return enabled;
     */
    private boolean checkIsCurrentRunHeaterEnabled() {
        return this.getEnableSignalChannel().getNextWriteValueAndReset().orElse(false);
    }

    /**
     * Check if any component isn't enabled anymore and references needs to be set again.
     */
    private void checkMissingComponents() {
        OpenemsComponent componentFetchedByCpm;
        try {
            if (this.isValve) {
                if (this.configuredValve.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.configuredValve.id());
                    if (componentFetchedByCpm instanceof Valve) {
                        this.configuredValve = (Valve) componentFetchedByCpm;
                    }
                }
            } else {
                if (this.configuredValveController.isEnabled() == false) {
                    componentFetchedByCpm = cpm.getComponent(this.configuredValveController.id());
                    if (componentFetchedByCpm instanceof ValveController) {
                        this.configuredValveController = (ValveController) componentFetchedByCpm;
                    }
                }
            }
            if (this.thresholdThermometer.isEnabled() == false) {
                componentFetchedByCpm = cpm.getComponent(this.thresholdThermometer.id());
                if (componentFetchedByCpm instanceof ThresholdThermometer) {
                    this.thresholdThermometer = (ThresholdThermometer) componentFetchedByCpm;
                }
            }
        } catch (OpenemsError.OpenemsNamedException ignored) {
            //TODO (?) Was soll passieren wenn Komponente nicht gefunden werden kann/falsche instanceof nur heaterstate --> error?
            this.setState(HeaterState.ERROR.name());
        }
    }

    /**
     * "deactivate" logic e.g. if heat is not needed anymore.
     * Channel Request -> false;
     * Release thresholdThermometer
     * if valve --> close (OR force close? @Pauli)
     * if ValveController --> force close or close?
     */
    void deactivateControlledComponents() {
        this.getNeedHeatChannel().setNextValue(false);
        this.getNeedMoreHeatChannel().setNextValue(false);
        this.thresholdThermometer.releaseSetPointTemperatureId(super.id());
        this.closeValveOrDisableValveController();
        this.setState(HeaterState.OFFLINE.name());
    }

    /**
     * When Called close the Valve (if configured) or otherwise disable the ValveController.
     */
    private void closeValveOrDisableValveController() {
        if (this.isValve) {
            this.configuredValve.setPowerLevelPercent().setNextValue(0);
        } else {
            this.configuredValveController.setEnableSignal(false);
        }
    }


    //_---------------------------------TODOS---------------------------------//


    //TODO IDK What to do here --> Override methods of Heater
    @Override
    public boolean hasError() {
        return this.errorInHeater();
    }

    @Override
    public void requestMaximumPower() {

    }

    @Override
    public void setIdle() {
    }

    @Override
    public boolean setPointPowerPercentAvailable() {
        return false;
    }

    @Override
    public boolean setPointPowerAvailable() {
        return false;
    }

    @Override
    public boolean setPointTemperatureAvailable() {
        return true;
    }

    @Override
    public int calculateProvidedPower(int demand, float bufferValue) throws OpenemsError.OpenemsNamedException {
        //TODO (?)
        return 0;
    }

    @Override
    public int getMaximumThermalOutput() {
        //TODO
        return 0;
    }

}
