package io.openems.edge.thermometer.threshold;

import io.openems.common.exceptions.OpenemsError;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.thermometer.api.Thermometer;
import io.openems.edge.thermometer.api.ThermometerState;
import io.openems.edge.thermometer.api.ThresholdThermometer;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This Component Helps to Handle Thermometer and the Thresholds etc.
 */
@Designate(ocd = Config.class, factory = true)
@Component(name = "Thermometer.Threshold",
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE})

public class ThresholdThermometerImpl extends AbstractOpenemsComponent implements OpenemsComponent, ThresholdThermometer, EventHandler {

    @Reference
    ComponentManager cpm;

    // Sometimes there are TemperatureFluctuations; The IntervalCounter prevents "jumping" of temperature
    // Example: The temperature is usually 500 dC; the maxInterval is (for easier explanation) 1
    // if for one cycle the temperature falls to 490 dC and the current interval counter is 0 ; nothing will happen
    // if in the next cycle the temperature stays at 490 temperature will be set, otherwise if temperature stays at 500 interval reset
    private AtomicInteger intervalToWaitCounter = new AtomicInteger(0);

    private int maxIntervalToWait;

    private Thermometer referenceThermometer;

    public ThresholdThermometerImpl() {
        super(OpenemsComponent.ChannelId.values(),
                Thermometer.ChannelId.values(),
                ThresholdThermometer.ChannelId.values());
    }

    @Activate
    public void activate(ComponentContext context, Config config) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        super.activate(context, config.id(), config.alias(), config.enabled());
        if (cpm.getComponent(config.thermometerId()) instanceof Thermometer) {
            this.referenceThermometer = cpm.getComponent(config.thermometerId());
            //Calculate Temperature at the beginning
            int referenceTemperature = referenceThermometer.getTemperatureValue();
            int threshold = config.thresholdTemperature();
            int fictionalTemperature = (referenceTemperature / threshold) * threshold;
            this.getTemperatureChannel().setNextValue(fictionalTemperature);
            //set config values
            this.setThresholdInDecidegree(config.thresholdTemperature());
            this.setThermometerState(ThermometerState.RISING);
            this.getSetPointTemperatureChannel().setNextValue(config.startSetPointTemperature());
            this.maxIntervalToWait = config.maxInterval();
            this.getSetPointTemperatureActiveChannel().setNextValue(false);
        } else {
            throw new ConfigurationException("Activate Method ThresholdTemperatureImpl " + config.id(),
                    "Activate Method: ThermometerId" + config.thermometerId() + " Not an Instance of Thermometer");
        }
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }


    /**
     * Assert that TemperatureSensor ist always active/enabled and correct
     * And update the Thermometer.
     *
     * @param event BeforeProcess Image
     */
    @Override
    public void handleEvent(Event event) {

        if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
            if (this.referenceThermometer != null) {
                if (!this.referenceThermometer.isEnabled()) {
                    try {
                        this.referenceThermometer = this.cpm.getComponent(this.referenceThermometer.id());
                    } catch (OpenemsError.OpenemsNamedException e) {
                        e.printStackTrace();
                    }
                }
                if (this.referenceThermometer.getTemperatureChannel().value().isDefined() && this.referenceThermometer.getTemperatureValue() != Integer.MIN_VALUE) {
                    calculateFictionalTemperatureValue();
                }
            }
        }
    }

    /**
     * Calculate the Fictional Temperature and sets to the TemperatureChannel.
     * Depends on: Falling or Rising Thermometer State; intervalCount; Threshold
     * <p>
     * Example: Lets say we have an initial Temperature of 500 dC (comes from a temperatureSensor)
     * And our Temperature is in the state of  RISING
     * Our Threshold is 10dC (1°C)
     * And we have an interval of 1 (1 fluctuation of our thresholdLimit is allowed)
     * Now our thermometer measures 495 dC
     * What happens to our fictional temperature? nothing
     * what happens if thermometer is 491? nothing
     * what happens if thermometer is 490 or 489? --> set temperature to 490 IF and only IF -> interval is at the limit
     * otherwise nothing happens
     * </p>
     * If the temperature is FALLING same thing applies.
     */
    private void calculateFictionalTemperatureValue() {
        int currentTemperature = this.getTemperatureValue();
        float thermometerValue = (float) this.referenceThermometer.getTemperatureValue();
        int incomingTemperature;
        boolean roundUp = false;
        float threshold = this.getThreshold();
        //IF temperature is 580 dC Threshold = 10dC ; Thermometer shows 574dC --> round up 574/10 = 58;
        // thermometer show 589dC <- rond down thermometer/threshold ---> 589/10 = 58;
        //Otherwise --> 574/10 would be 57 and 589/10 stays 58
        //@Paul ist das so korrekt?
        if (thermometerValue < currentTemperature) {
            roundUp = true;
        }
        // now i know how to round -> ceil or floor
        incomingTemperature = roundUp ? (int) (Math.ceil(thermometerValue / threshold) * threshold)
                : (int) (Math.floor(thermometerValue / threshold) * threshold);
        //Check if it's not equals --> e.g. 580 dC was before now its 570dC
        if (incomingTemperature != currentTemperature) {
            //Check if I have to wait until i can set the new Temperature
            if (this.intervalToWaitCounter.get() < this.maxIntervalToWait) {
                this.intervalToWaitCounter.getAndIncrement();
            } else {
                //Apply new Values -> Is Thermometer rising/falling, whats the new temperature etc etc
                boolean falling = incomingTemperature < currentTemperature;
                ThermometerState newThermometerState = ThermometerState.RISING;
                if (falling) {
                    newThermometerState = ThermometerState.FALLING;
                }
                this.getTemperatureChannel().setNextValue(incomingTemperature);
                this.setThermometerState(newThermometerState);
                this.intervalToWaitCounter.getAndSet(0);
            }
        } else {
            this.intervalToWaitCounter.getAndSet(0);
        }
    }

    @Override
    public String debugLog() {
        return "T: " + (this.getTemperatureValue() == Integer.MIN_VALUE ? "NotDefined" : this.getTemperatureValue()) + " dC";
    }
}
