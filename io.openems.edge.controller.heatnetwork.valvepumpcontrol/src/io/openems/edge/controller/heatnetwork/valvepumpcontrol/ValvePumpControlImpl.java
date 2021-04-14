package io.openems.edge.controller.heatnetwork.valvepumpcontrol;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.controlcenter.api.ControlCenter;
import io.openems.edge.controller.heatnetwork.valvepumpcontrol.api.ValvePumpControl;
import io.openems.edge.heatsystem.components.api.Pump;
import io.openems.edge.heatsystem.components.api.Valve;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
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

import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This controller opens a valve and activates a pump when it receives the signal that the heater wants to heat.
 * There is also an override channel to manage access to the valve by another controller. The override channel
 * has priority for valve control over the heater signal.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "ValvePumpControl", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class ValvePumpControlImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller, ValvePumpControl {

    private final Logger log = LoggerFactory.getLogger(ValvePumpControlImpl.class);

    @Reference
    protected ComponentManager cpm;

    private Config config;

    private ControlCenter heatingController;
    private Valve valveUS01;
    private Pump pumpHK01;
    private Thermometer thermometerFlow;
    private Thermometer thermometerReturn;
    private int closeValvePercent = 20;
    private int timeToResetSeconds = 20;
    private int openedPercentLast = 0;
    // Variables for channel readout
    private boolean heaterWantsToHeat;
    private boolean valveOverrideActive;


    public ValvePumpControlImpl() {
        super(OpenemsComponent.ChannelId.values(),
                ValvePumpControl.ChannelId.values(),
                Controller.ChannelId.values());
    }


    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        AtomicBoolean instanceFound = new AtomicBoolean(false);

        cpm.getAllComponents().stream().filter(component -> component.id().equals(config.id())).findFirst().ifPresent(consumer -> {
            instanceFound.set(true);
        });
        if (instanceFound.get() == true) {
            return;
        }

        this.config = config;
        super.activate(context, config.id(), config.alias(), config.enabled());

        this.noError().setNextValue(true);
        this.closeValvePercent = config.closeValvePercent();
        this.timeToResetSeconds = config.timeToResetSeconds();

        //allocate components
        allocateComponents();

        this.valveClosing().setNextValue(config.closeValvePercent());
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
        valveUS01.forceClose();
        if (pumpHK01 != null) {
            pumpHK01.changeByPercentage(-101); // = deactivate. Use 101 because variable is double and math with double is not accurate.

        }
    }

    void allocateComponents() throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (cpm.getComponent(config.allocated_Heating_Controller()) instanceof ControlCenter) {
            heatingController = cpm.getComponent(config.allocated_Heating_Controller());
        } else {
            throw new ConfigurationException(config.allocated_Heating_Controller(),
                    "Allocated Heating Controller not a Heating Controller; Check if Name is correct and try again.");
        }
        if (cpm.getComponent(config.valveUS01Id()) instanceof Valve) {
            valveUS01 = cpm.getComponent(config.valveUS01Id());
        } else {
            throw new ConfigurationException("The configured component is not a valve! Please check "
                    + config.valveUS01Id(), "configured component is incorrect!");
        }
        try {//optional pump
            if (cpm.getComponent(config.pumpHK01Id()) instanceof Pump) {
                pumpHK01 = cpm.getComponent(config.pumpHK01Id());
            }
        } catch (Exception e) {
        }

        try {//optional thermometer
            if (cpm.getComponent(config.temperatureSensorFlow()) instanceof Thermometer) {
                thermometerFlow = cpm.getComponent(config.temperatureSensorFlow());
            }
            if (cpm.getComponent(config.temperatureSensorReturn()) instanceof Thermometer) {
                thermometerReturn = cpm.getComponent(config.temperatureSensorReturn());
            }
        } catch (Exception e) {
        }


    }

    @Reference
    ConfigurationAdmin ca;

    private void updateConfig() {
        Configuration c;

        try {
            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            if (this.valveClosing().value().isDefined()) {
                properties.put("valve.closing", this.valveClosing().value().get());
                closeValvePercent = this.valveClosing().value().get();
            }
            c.update(properties);
        } catch (IOException e) {
        }
    }

    private long nochangeTime = 0;

    private void checkRestart() {
        if (thermometerFlow != null && thermometerReturn != null) {
            if (20 > thermometerFlow.getTemperature().value().get() - thermometerReturn.getTemperature().value().get()) {
                if (nochangeTime == 0) {
                    nochangeTime = System.currentTimeMillis();
                } else {
                    if (System.currentTimeMillis() - nochangeTime > timeToResetSeconds * 1000) {
                        valveUS01.reset();
                        nochangeTime = 0;
                    }
                }
            } else {
                nochangeTime = 0;
            }
        }
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        boolean restchange = this.valveClosing().getNextWriteValueAndReset().isPresent();
        if (componentIsMissing()) {
            log.warn("A Component is missing in: " + super.id());
            return;
        }
        if (restchange) {
            updateConfig();
        }

        // Transfer channel data to local variables for better readability of logic code.
        heaterWantsToHeat = heatingController.activateHeater().value().orElse(false);    // Null in channel is counted as false.
        valveOverrideActive = activateValveOverride().value().orElse(false); // Null in channel is counted as false.
        int actualValvePercent = valveUS01.getPowerLevel().value().get().intValue();
        if (actualValvePercent == openedPercentLast) {
            checkRestart();
        }
        // Control logic.
        // If heating controller says it wants to heat, open valve and activate pump.
        // When no heating is required, close valve and stop pump.
        if (valveOverrideActive) {
            executeOverrideCommand();
        } else {
            if (heaterWantsToHeat) {
                valveOpen();
            } else {
                //
                valveClose(closeValvePercent);
            }
        }
        if (heaterWantsToHeat) {
            startPump();
        } else {
            stopPump();
        }
    }

    private boolean componentIsMissing() {
        try {
            if (this.heatingController.isEnabled() == false) {
                this.heatingController = cpm.getComponent(config.allocated_Heating_Controller());
            }
            if (this.valveUS01.isEnabled() == false) {
                this.valveUS01 = cpm.getComponent(config.valveUS01Id());
            }
            if (this.pumpHK01 != null && this.pumpHK01.isEnabled() == false) {
                this.pumpHK01 = cpm.getComponent(config.pumpHK01Id());
            }
            return false;
        } catch (OpenemsError.OpenemsNamedException e) {
            return true;
        }
    }

    private void valveClose(int percent) {
        // Check if valve is already closed. If there is null in the channel, it's probably offline and closed.
        boolean isValveBusy = valveUS01.getIsBusy().value().isDefined() && valveUS01.getIsBusy().value().get();
        if (isValveBusy || valveUS01.getPowerLevel().value().get() < percent) {
            return;
        }

        valveUS01.changeByPercentage(percent - valveUS01.getPowerLevel().value().get());

    }

    private void valveOpen() {
        // Check if valve is operational. If there is null in power level channel, something is wrong.
        if (valveUS01.getPowerLevel().value().isDefined()) {
            // Check if valve is already open.
            if (valveUS01.getPowerLevel().value().get() < 100) {

                boolean isValveBusy = valveUS01.getIsBusy().value().isDefined() && valveUS01.getIsBusy().value().get();
                if (isValveBusy) {
                    return;
                }
                valveUS01.changeByPercentage(100 - valveUS01.getPowerLevel().value().get());
            }
            this.noError().setNextValue(true);
        } else {
            this.noError().setNextValue(false);
            this.logError(this.log, "ERROR: null in valve power level channel. Something must be wrong with the valve!");
        }
    }

    private void stopPump() {
        // Check if pump has already stopped.
        if (closeValvePercent == 0 && pumpHK01 != null && pumpHK01.getPowerLevel().value().orElse(0.0) > 0) {
            pumpHK01.changeByPercentage(-101); // = deactivate. Use 101 because variable is double and math with double is not accurate.
        }
    }

    // Pump goes to full speed.
    private void startPump() {
        if (pumpHK01 != null) {
            // Check if pump is operational. If there is null in power level channel, something is wrong.
            if (pumpHK01.getPowerLevel().value().isDefined()) {
                // Check if pump is already at full power.
                if (pumpHK01.getPowerLevel().value().get() < 100) {
                    pumpHK01.changeByPercentage(100); // = full power.
                }
                this.noError().setNextValue(true);
            } else {
                this.noError().setNextValue(false);
                this.logError(this.log, "ERROR: null in pump power level channel. Something must be wrong with the pump!");
            }
        }
    }

    // The override opens or closes the valve.
    private void executeOverrideCommand() {
        // Check if there is a value.
        if (setValveOverrideOpenClose().value().isDefined()) {
            // True means open
            if (setValveOverrideOpenClose().value().get()) {
                valveOpen();
            } else {
                valveClose(closeValvePercent);
            }
        }
    }

}


