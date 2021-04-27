package io.openems.edge.controller.heatnetwork.fallbackactivate;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.relay.api.Relay;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * This controller activates a fallback heater via a relay when a monitored temperature drops below a certain value.
 *
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "temperature.controller.fallbackactivate")
public class ControllerFallbackactivateImpl extends AbstractOpenemsComponent implements OpenemsComponent, Controller {

    private final Logger log = LoggerFactory.getLogger(ControllerFallbackactivateImpl.class);

    @Reference
    protected ComponentManager cpm;

    private Thermometer tempSensor;
    private Relay relay;
    private int minTemp;
    private boolean displayOnce;
    private boolean error;
    private int hysteresis;

    // Variables for channel readout
    private boolean tempSensorSendsData;
    private int temperature;

    public ControllerFallbackactivateImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Controller.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());

        // Convert to dezidegree, since temperature sensor data is in dezidegree as well.
        minTemp = config.min_temp() * 10;
        hysteresis = config.hysteresis() * 10;

        displayOnce = false;
        error = false;

        // Allocate components.
        try {
            if (cpm.getComponent(config.temp_Sensor()) instanceof Thermometer) {
                tempSensor = cpm.getComponent(config.temp_Sensor());
            } else {
                throw new ConfigurationException(config.temp_Sensor(), "The temperature-sensor " + config.temp_Sensor()
                        + " is not a (configured) temperature sensor.");
            }
            if (cpm.getComponent(config.relay_id()) instanceof Relay) {
                relay = cpm.getComponent(config.relay_id());
                // Set relay to "off" state upon initialization.
                controlRelay(false);
            } else {
                throw new ConfigurationException(config.relay_id(), "The allocated relay " + config.relay_id()
                        + " is not a (configured) relay.");
            }
        } catch (OpenemsError.OpenemsNamedException | ConfigurationException e) {
            e.printStackTrace();
            throw e;
        }
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        // Transfer channel data to local variables for better readability of logic code.
        tempSensorSendsData = tempSensor.getTemperatureChannel().value().isDefined();
        if (tempSensorSendsData) {
            temperature = tempSensor.getTemperatureChannel().value().get();

            // Error handling
            if (error) {
                error = false;
                this.logInfo(this.log, "Everything is fine now! Reading from the temperature sensor is "
                        + temperature / 10 + "°C.");
            }
        } else {
            // You land here when there is no data from the temperature sensor (null in channel). -> Error
            error = true;
            this.logError(this.log, "ERROR: Not getting any data from the temperature sensor!");
        }


        // Control logic.
        if (tempSensorSendsData) {
            if (temperature < minTemp) {
                controlRelay(true);
                if (displayOnce == false) {
                    this.logInfo(this.log, "Fallback heater activated. Activation temperature is "
                            + minTemp / 10 + "°C, measured temperature is " + temperature / 10 + "°C.");
                    displayOnce = true;
                }
            } else {
                if (temperature >= minTemp + hysteresis) {
                    controlRelay(false);
                    if (displayOnce) {
                        this.logInfo(this.log, "Fallback heater deactivated. Activation temperature plus hysteresis is "
                                + (minTemp + hysteresis) / 10 + "°C, measured temperature is " + temperature / 10 + "°C.");
                        displayOnce = false;
                    }
                }
            }
        }
    }

    public void controlRelay(boolean activate) {
        try {
            if (relay.isCloser().value().get()) {
                relay.getRelaysWriteChannel().setNextWriteValue(activate);
            } else {
                relay.getRelaysWriteChannel().setNextWriteValue(!activate);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            e.printStackTrace();
        }
    }

}
