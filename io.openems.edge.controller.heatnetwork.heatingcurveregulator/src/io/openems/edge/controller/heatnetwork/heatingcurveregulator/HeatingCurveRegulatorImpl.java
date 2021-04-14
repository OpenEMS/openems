package io.openems.edge.controller.heatnetwork.heatingcurveregulator;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.heatingcurveregulator.api.HeatingCurveRegulatorChannel;
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
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the Consolinno weather dependent heating controller.
 * - It takes the outside temperature as input and asks for the heating to be turned on or off based on the outside
 * temperature.
 * - If the outside temperature is below the activation threshold, a heating temperature is calculated based
 * on a parametrized heating curve.
 */

@Designate(ocd = Config.class, factory = true)
@Component(name = "AutomaticRegulator", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class HeatingCurveRegulatorImpl extends AbstractOpenemsComponent implements OpenemsComponent, HeatingCurveRegulatorChannel, Controller {

    private final Logger log = LoggerFactory.getLogger(HeatingCurveRegulatorImpl.class);

    @Reference
    protected ComponentManager cpm;

    private Thermometer outsideTempSensor;
    private int activationTemp;
    private int roomTemp;
    private double slope;
    private int offset;
    private LocalDateTime timestamp;
    private int measurementTimeMinutes;
    private int minimumStateTimeMinutes;
    private boolean measureAverage = false;
    private int measurementCounter = 0;
    private int[] measurementDataOneMinute = new int[60];
    private List<Integer> measurementData = new ArrayList<>();
    private boolean shouldBeHeating = false;

    // Variables for channel readout
    private boolean tempSensorSendsData;
    private int outsideTemperature;

    public HeatingCurveRegulatorImpl() {
        super(OpenemsComponent.ChannelId.values(),
                HeatingCurveRegulatorChannel.ChannelId.values(),
                Controller.ChannelId.values());
    }

    private boolean initial = true;

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
        initial = true;
        activationTemp = config.activation_temp();
        roomTemp = config.room_temp();
        // Activation temperature can not be higher than desired room temperature, otherwise the function will crash.
        if (activationTemp > roomTemp) {
            activationTemp = roomTemp;
        }
        // Convert to dezidegree, since sensor data is dezidegree too.
        activationTemp = activationTemp * 10;
        slope = config.slope();
        offset = config.offset();

        this.noError().setNextValue(true);

        // Set timestamp so that logic part 1 executes right away.
        timestamp = LocalDateTime.now().minusMinutes(minimumStateTimeMinutes);

        measurementTimeMinutes = config.measurement_time_minutes();
        minimumStateTimeMinutes = config.minimum_state_time_minutes();
        if (measurementTimeMinutes > minimumStateTimeMinutes) {
            measurementTimeMinutes = minimumStateTimeMinutes;
        }

        measureAverage = false;
        measurementCounter = 0;
        measurementData.clear();

        // Allocate temperature sensor.

        if (cpm.getComponent(config.temperatureSensorId()) instanceof Thermometer) {

            this.outsideTempSensor = cpm.getComponent(config.temperatureSensorId());
        } else {
            throw new ConfigurationException("The configured component is not a temperature sensor! Please check "
                    + config.temperatureSensorId(), "configured component is incorrect!");
        }


        this.getRoomTemperature().setNextValue(config.room_temp());
        this.getActivationTemperature().setNextValue(config.activation_temp());
        this.getSlope().setNextValue(config.slope());
        this.getOffset().setNextValue(config.offset());
    }


    @Deactivate
    public void deactivate() {
        super.deactivate();
        turnOnHeater(false);
    }

    @Reference
    ConfigurationAdmin ca;

    private void updateConfig() {
        Configuration c;

        try {
            c = ca.getConfiguration(this.servicePid(), "?");
            Dictionary<String, Object> properties = c.getProperties();
            Optional t = this.getRoomTemperature().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("room.temp", t.get());
            }
            t = this.getActivationTemperature().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("activation.temp", t.get());
            }
            t = this.getSlope().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("slope", t.get());
            }
            t = this.getOffset().getNextWriteValueAndReset();
            if (t.isPresent()) {
                properties.put("offset", t.get());
            }
            c.update(properties);

        } catch (IOException e) {
        }
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {

        boolean restchange = this.getActivationTemperature().getNextWriteValue().isPresent();
        restchange |= this.getRoomTemperature().getNextWriteValue().isPresent();
        restchange |= this.getSlope().getNextWriteValue().isPresent();
        restchange |= this.getOffset().getNextWriteValue().isPresent();
        if (restchange) {
            updateConfig();
        }
        // Transfer channel data to local variables for better readability of logic code.
        tempSensorSendsData = outsideTempSensor.getTemperatureChannel().value().isDefined();
        if (tempSensorSendsData) {
            outsideTemperature = outsideTempSensor.getTemperatureChannel().value().get();

            // Error handling.
            if (this.noError().value().get() == false) {
                this.noError().setNextValue(true);
                this.logInfo(this.log, "Everything is fine now! Reading from the temperature sensor is "
                        + outsideTemperature / 10 + "°C.");
            }
        } else {
            // No data from the temperature sensor (null in channel). -> Error
            turnOnHeater(false);
            this.noError().setNextValue(false);
            this.logError(this.log, "Not getting any data from the outside temperature sensor " + outsideTempSensor.id() + ".");
        }


        // Control logic. Execution starts at part 1, which decides if the next part executes or not.
        if (tempSensorSendsData) {

            // Part 1. Test temperature. Execution blocked by timestamp that is set when state changes (heating or no
            // heating). This means once state changes, it will keep that state for at least minimumStateTimeMinutes.
            if (shouldBeHeating && ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) > minimumStateTimeMinutes) {
                // Check if temperature is above activationTemp
                if (outsideTemperature > activationTemp) {
                    measureAverage = true;
                    timestamp = LocalDateTime.now();
                    measurementCounter = 0;
                    measurementData.clear();
                }
            }
            if (initial || (shouldBeHeating == false && ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) > minimumStateTimeMinutes)) {
                initial = false;
                // Check if temperature is below activationTemp
                if (outsideTemperature <= activationTemp) {
                    measureAverage = true;
                    timestamp = LocalDateTime.now();
                    measurementCounter = 0;
                    measurementData.clear();
                }
            }
            // Part 2. Get average temperature over a set time period (entered in config). Use that average to decide
            // heating state. Has a shortcut that decides faster if average temperature of 60 cycles is well above or
            // below activation temperature. The shortcut is for a controller restart in winter, so heating starts faster.
            if (measureAverage) {
                measurementDataOneMinute[measurementCounter] = outsideTemperature;
                measurementCounter++;
                if (measurementCounter >= 60) {
                    double average = 0;
                    for (int i = 0; i < 60; i++) {
                        average += measurementDataOneMinute[i];
                    }
                    average = average / 60.0;
                    measurementData.add((int) Math.round(average));
                    measurementCounter = 0;

                    // Shortcut if average of one minute is 5k above or below activationTemp.
                    if (shouldBeHeating) {
                        if (average > activationTemp + 30) {
                            shouldBeHeating = false;
                            timestamp = LocalDateTime.now();
                            measureAverage = false;
                        }
                    } else {
                        if (average <= activationTemp - 30) {
                            shouldBeHeating = true;
                            timestamp = LocalDateTime.now();
                            measureAverage = false;
                        }
                    }
                }

                // Part 3. Evaluation at end of measurement time.
                // Fail safe: needs at least one entry in measurementData, meaning at least 60 cycles have passed.
                if (ChronoUnit.MINUTES.between(timestamp, LocalDateTime.now()) > measurementTimeMinutes
                        && measurementData.size() >= 1) {
                    double sum = 0;
                    for (Integer entry : measurementData) {
                        sum += entry;
                    }
                    int totalAverage = (int) Math.round(sum / measurementData.size());

                    if (shouldBeHeating) {
                        // Is heating right now. Should heating be turned off?
                        if (totalAverage > activationTemp) {
                            shouldBeHeating = false;
                            timestamp = LocalDateTime.now();
                        } else {
                            // Set timestamp so that part 1 executes again right away.
                            timestamp = LocalDateTime.now().minusMinutes(minimumStateTimeMinutes);
                        }
                    } else {
                        // Is not heating right now. Should heating be turned on?
                        if (totalAverage <= activationTemp) {
                            shouldBeHeating = true;
                            timestamp = LocalDateTime.now();
                        } else {
                            // Set timestamp so that part 1 executes again right away.
                            timestamp = LocalDateTime.now().minusMinutes(minimumStateTimeMinutes);
                        }
                    }
                    measureAverage = false;
                }
            }


            if (shouldBeHeating) {
                turnOnHeater(true);

                // Calculate heating temperature. Function calculates everything in degree, not dezidegree!
                double function = (slope * 1.8317984 * Math.pow((roomTemp - (0.1 * outsideTemperature)), 0.8281902))
                        + roomTemp + offset;

                // Convert back to dezidegree integer.
                int outputTempDezidegree = (int) Math.round(function * 10);

                setHeatingTemperature(outputTempDezidegree);
                this.logDebug(this.log, "Outside thermometer measures " + 0.1 * outsideTemperature
                        + "°C. Heater function calculates forward temperature to " + outputTempDezidegree / 10 + "°C.");
            } else {
                turnOnHeater(false);
            }
        }
    }

    private void turnOnHeater(boolean activate) {
        this.signalTurnOnHeater().setNextValue(activate);
    }

    private void setHeatingTemperature(int temperature) {
        this.getHeatingTemperature().setNextValue(temperature);
    }

}
