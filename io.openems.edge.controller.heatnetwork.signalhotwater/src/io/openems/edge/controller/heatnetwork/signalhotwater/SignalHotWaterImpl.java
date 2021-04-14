package io.openems.edge.controller.heatnetwork.signalhotwater;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.signalhotwater.api.SignalHotWater;
import io.openems.edge.thermometer.api.Thermometer;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Dictionary;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;


/**
 * This is the Consolinno SignalHotWater Controller. It monitors two temperature sensors on a water tank and listens
 * to a remote signal about the heat network status. The water tank upper sensor monitors the minimum temperature, the
 * water tank lower sensor monitors the maximum temperature. When the temperature falls below the minimum, the
 * controller sets the heatTankRequest channel to "true", which signals to a remote station to activate the heat network.
 * The controller then waits for a response from the remote station that the heat network is ready ("true" is written in
 * the heatNetworkReadySignal channel by the remote station), or proceeds after a timeout. Then the startHeatingTank
 * channel is set to "true", which signals to the next controller to start transferring heat from the heat network to
 * the water tank.
 * While the water tank temperature is within bounds, changes (!) in the heatNetworkReadySignal channel are
 * passed on to the startHeatingTank channel. That means, if the heatNetworkReadySignal channel changes from "true" to
 * "false" while the water tank is heating up and is already above min temperature, startHeatingTank will change to "false"
 * and the heating will stop. If heatNetworkReadySignal changes from "false" to "true" while the tank is below max
 * temperature, startHeatingTank will change to "true" and heating will start.
 */

// An Paul:
// Anmerkung zum Controller: Wenn das Nahwärme Netzwerk nicht anspringt (warum auch immer) wird bei aktueller Logik
// versucht den Wassertank zu heizen bis er voll ist. Solange ist die Heizung abgeklemmt. Reicht die Wärme im Netzwerk
// nicht um den Wassertank voll zu bekommen, bleibt der Controller im "Wassertank Heizen" Status hängen und die Heizung
// bleibt abgeklemmt. (Keine Ahnung ob das tatsächlich eintreten kann.)
// Also Wasser lauwarm und Heizung ganz aus wenn die Nahwärme nicht reicht.
// Wäre Heizung lauwarm und Wasser kalt da nicht besser? Im Winter vermutlich ja, im Sommer nein.
// Eine Lösung wäre ein Timer, der das befüllen vom Wassertank irgendwann beendet, wenn das Wärmenetz nicht an geht.
// Kann auch mit der Außentemperatur gekoppelt werden, um dieses Errorhandling auf den Winter zu begrenzen.
//
// Andere Lösung: Wenn das remote Signal heatNetworkReady kurz an und wieder aus geht würde das Tankbefüllen auch
// abgebrochen. Sofern der Wassertank über minimum Temperatur ist.


@Designate(ocd = Config.class, factory = true)
@Component(name = "SignalHotWater", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SignalHotWaterImpl extends AbstractOpenemsComponent implements OpenemsComponent, SignalHotWater, Controller {

    private final Logger log = LoggerFactory.getLogger(SignalHotWaterImpl.class);

    @Reference
    protected ComponentManager cpm;

    private Thermometer watertankTempSensorUpperChannel;
    private Thermometer watertankTempSensorLowerChannel;
    private int minTempUpper;
    private int maxTempLower;
    private int responseTimeout;
    private LocalDateTime timestamp;
    private boolean heatNetworkStateTracker;

    // Variables for channel readout
    private boolean tempSensorsSendData;
    private boolean heatNetworkSignalReceived;
    private boolean heatNetworkReady;
    private int watertankTempUpperSensor;
    private int watertankTempLowerSensor;
    private long startTimeNotTooCold = 0;
    private int heatingDelay = 0;

    public SignalHotWaterImpl() {
        super(OpenemsComponent.ChannelId.values(),
                SignalHotWater.ChannelId.values(),
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
        super.activate(context, config.id(), config.alias(), config.enabled());

        minTempUpper = config.min_temp_upper() * 10;    // Convert to dezidegree.
        maxTempLower = config.max_temp_lower() * 10;
        heatingDelay = config.getHeatingDelay() * 1000;
        responseTimeout = config.response_timeout();
        this.heatTankRequest().setNextValue(false);
		this.startHeatingTank().setNextValue(false);
        heatNetworkStateTracker = false;
        this.noError().setNextValue(true);

        // Allocate components.
        if (cpm.getComponent(config.temperatureSensorUpperId()) instanceof Thermometer) {
            this.watertankTempSensorUpperChannel = cpm.getComponent(config.temperatureSensorUpperId());
        } else {
            throw new ConfigurationException("The configured component is not a temperature sensor! Please check "
                    + config.temperatureSensorUpperId(), "configured component is incorrect!");
        }
        if (cpm.getComponent(config.temperatureSensorLowerId()) instanceof Thermometer) {
            this.watertankTempSensorLowerChannel = cpm.getComponent(config.temperatureSensorLowerId());
        } else {
            throw new ConfigurationException("The configured component is not a temperature sensor! Please check "
                    + config.temperatureSensorLowerId(), "configured component is incorrect!");
        }

        this.minTempUpper().setNextValue(config.min_temp_upper());
        this.maxTempLower().setNextValue(config.max_temp_lower());

    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
    }

    @Reference
    ConfigurationAdmin ca;

    private void updateConfig() {
        Configuration c;

        try {
            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();

            Optional t = this.maxTempLower().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("max.temp.lower", t.get());
            }
            t = this.minTempUpper().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("min.temp.upper", t.get());
            }
            c.update(properties);
        } catch (IOException e) {
        }
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        boolean restchange = this.maxTempLower().getNextWriteValue().isPresent();
        restchange |= this.minTempUpper().getNextWriteValue().isPresent();
        if (restchange) {
            updateConfig();
        }
        // Sensor checks and error handling.
        if (watertankTempSensorUpperChannel.getTemperatureChannel().value().isDefined()) {
            if (watertankTempSensorLowerChannel.getTemperatureChannel().value().isDefined()) {
                tempSensorsSendData = true;
                watertankTempLowerSensor = watertankTempSensorLowerChannel.getTemperatureChannel().value().get();
                watertankTempUpperSensor = watertankTempSensorUpperChannel.getTemperatureChannel().value().get();
                if (this.noError().value().get() == false) {
                    this.noError().setNextValue(true);
                    this.logInfo(this.log, "Temperature sensors are fine now!");
                }
            } else {
                tempSensorsSendData = false;
                this.noError().setNextValue(false);
                this.logError(this.log, "Not getting any data from the water tank lower temperature sensor " + watertankTempSensorLowerChannel.id() + ".");
            }
        } else {
            tempSensorsSendData = false;
            this.noError().setNextValue(false);
            this.logError(this.log, "Not getting any data from the water tank upper temperature sensor " + watertankTempSensorUpperChannel.id() + ".");
        }

        // Transfer channel data to local variables for better readability of logic code.
        heatNetworkSignalReceived = this.heatNetworkReadySignal().value().isDefined();
        if (heatNetworkSignalReceived) {
            heatNetworkReady = this.heatNetworkReadySignal().value().get();
        }


        if (tempSensorsSendData) {

            // Check if water tank is above max temperature.
            if (watertankTempLowerSensor > maxTempLower && (startTimeNotTooCold != 0) && (startTimeNotTooCold + heatingDelay < System.currentTimeMillis())) {
                // Stop heating
                this.heatTankRequest().setNextValue(false);
                this.startHeatingTank().setNextValue(false);
            } else {

                // When water tank temperature is below max, check remote for "on" signal.
                // heatNetworkReady changes from false to true -> start heating.
                if (heatNetworkSignalReceived && heatNetworkReady && (heatNetworkStateTracker == false)) {
                    this.heatTankRequest().setNextValue(true);
                    heatNetworkStateTracker = true;

                    // Set timer, just in case "heatNetworkReady == false" next cycle.
                    timestamp = LocalDateTime.now();
                    startTimeNotTooCold = 0;
                }

                // Check if heating has been requested.
                if (this.heatTankRequest().value().get()) {

                    // Wait for "heatNetworkReady == true" or execute after timeout.
                    if ((heatNetworkSignalReceived && heatNetworkReady)
                            || ChronoUnit.SECONDS.between(timestamp, LocalDateTime.now()) >= responseTimeout) {

                        // Check if we got here by timeout.
                        if ((heatNetworkSignalReceived && heatNetworkReady) == false) {
                            if (this.heatTankRequest().value().get() == false) {
                                this.logWarn(this.log, "Warning: heat network response timeout.");
                            }
                        }

                        this.startHeatingTank().setNextValue(true);
                    }
                }
            }

            // Check if water tank is too cold.
            if (watertankTempUpperSensor < minTempUpper) {

                // Request heat and start timer.
                if (this.heatTankRequest().value().get() == false) {
                    timestamp = LocalDateTime.now();
                    this.heatTankRequest().setNextValue(true);
                    startTimeNotTooCold = 0;

                }
            } else {
                if (startTimeNotTooCold == 0) {
                    startTimeNotTooCold = System.currentTimeMillis();
                }
                // If water tank temperature is not too low, check remote for "off" signal.
                // heatNetworkReady changes from true to false -> turn off heating.
                if (heatNetworkSignalReceived && (heatNetworkReady == false) && heatNetworkStateTracker) {
                    this.heatTankRequest().setNextValue(false);
                    heatNetworkStateTracker = false;
                }
            }

            // Pass on temperature of lower sensor. Saves needing to configure the temperature sensor in other controller.
            this.waterTankTemp().setNextValue(watertankTempLowerSensor);
        }


    }

}