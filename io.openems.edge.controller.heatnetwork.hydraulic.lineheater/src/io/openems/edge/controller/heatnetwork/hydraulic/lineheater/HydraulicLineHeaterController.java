package io.openems.edge.controller.heatnetwork.hydraulic.lineheater;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.LineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.api.HydraulicLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ChannelLineHeater;
import io.openems.edge.controller.heatnetwork.hydraulic.lineheater.helperclass.ValveLineHeater;
import io.openems.edge.heater.decentral.api.DecentralHeater;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;


@Designate(ocd = Config.class, factory = true)
@Component(name = "HydraulicLineHeaterController",
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        immediate = true)
public class HydraulicLineHeaterController extends AbstractOpenemsComponent implements OpenemsComponent, Controller, HydraulicLineHeater {

    private final Logger log = LoggerFactory.getLogger(HydraulicLineHeaterController.class);

    @Reference
    protected ComponentManager cpm;

    private Thermometer tempSensorReference;
    private DecentralHeater decentralHeaterOptional;

    //30sec remotesignal
    private int timeoutMaxRemote = 30;
    // all 10 Minutes check on Fallback
    private int timeoutRestartCycle = 600;
    private int temperatureDefault = 600;
    private boolean shouldFallback = false;
    private boolean isFallback = false;
    private DateTime initalTimeStampFallback;
    private int minuteFallbackStart;
    private int minuteFallbackStop;
    private static final String TYPE_TEMPERATURE = "Temperature";
    private static final String TYPE_VALVE = "Valve";
    private static final String TYPE_DECENTRAL_HEATER = "DecentralHeater";
    private static final String CONCRETE_TYPE_TEMPERATURE_SENSOR_REFERENCE = "tempSensorReference";
    private static final String CONCRETE_TYPE_VALVE_BYPASS = "valveBypass";
    private static final String CONCRETE_TYPE_DECENTRAL_HEATER_OPTIONAL = "decentralHeaterOptional";
    private static final int DEFAULT_TEMPERATURE = -127;
    private LineHeater lineHeater;
    //NOTE: If more Variation comes --> create extra "LineHeater"Classes in this controller etc


    public HydraulicLineHeaterController() {

        super(OpenemsComponent.ChannelId.values(),
                HydraulicLineHeater.ChannelId.values(),
                Controller.ChannelId.values());

    }

    @Activate
    public void activate(ComponentContext context, Config config) throws Exception {

        super.activate(context, config.id(), config.alias(), config.enabled());
        this.tempSensorReference = (Thermometer) allocateComponent(config.tempSensorReference(), TYPE_TEMPERATURE, CONCRETE_TYPE_TEMPERATURE_SENSOR_REFERENCE);
        if (config.valveOrChannel().equals(TYPE_VALVE)) {
            createLineHeater(config.valueToWriteIsBoolean(), config.valveBypass());
        } else {
            if (config.channels().length != 2) {
                throw new ConfigurationException("ChannelSize", "ChannelSize should be 2 but is : " + config.channels().length);
            }
            createLineHeater(config.valueToWriteIsBoolean(), config.channels());

        }
        if (config.useDecentralHeater()) {
            this.decentralHeaterOptional = (DecentralHeater) allocateComponent(config.decentralheaterReference(),
                    TYPE_DECENTRAL_HEATER, CONCRETE_TYPE_DECENTRAL_HEATER_OPTIONAL);
        }

        this.timeoutMaxRemote = config.timeoutMaxRemote();
        this.temperatureDefault = config.temperatureDefault();
        this.timeoutRestartCycle = config.timeoutRestartCycle();
        this.shouldFallback = config.shouldFallback();
        this.minuteFallbackStart = config.minuteFallbackStart();
        this.minuteFallbackStop = config.minuteFallbackStop();

    }

    private void createLineHeater(boolean b, String[] channels) throws OpenemsError.OpenemsNamedException {
        ChannelAddress readAddress = ChannelAddress.fromString(channels[0]);
        ChannelAddress writeAddress = ChannelAddress.fromString(channels[1]);
        this.lineHeater = new ChannelLineHeater(b, readAddress, writeAddress, this.cpm);
    }

    private void createLineHeater(boolean b, String s) throws Exception {
        Valve valve = (Valve) allocateComponent(s, TYPE_VALVE, CONCRETE_TYPE_VALVE_BYPASS);
        this.lineHeater = new ValveLineHeater(b, valve);
    }

    private OpenemsComponent allocateComponent(String device, String type, String concreteType) throws Exception {


        switch (type) {
            case TYPE_VALVE:
                return allocateValve(device, concreteType);
            case TYPE_TEMPERATURE:
                return allocateTemperatureSensor(device, concreteType);
            case TYPE_DECENTRAL_HEATER:
                return allocateDecentralHeater(device, concreteType);
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }

    private OpenemsComponent allocateDecentralHeater(String device, String concreteType) throws Exception {
        if (cpm.getComponent(device) instanceof DecentralHeater) {
            switch (concreteType) {
                case CONCRETE_TYPE_DECENTRAL_HEATER_OPTIONAL:
                    return cpm.getComponent(device);

            }
        }
        throw new Exception("Internal Error, this shouldn't occur");
    }

    private OpenemsComponent allocateValve(String device, String concreteType) throws Exception {
        if (cpm.getComponent(device) instanceof Valve) {
            switch (concreteType) {
                case CONCRETE_TYPE_VALVE_BYPASS:
                    return cpm.getComponent(device);
            }
        }
        throw new Exception("Internal Error, this shouldn't occur");

    }


    private OpenemsComponent allocateTemperatureSensor(String device, String concreteType) throws OpenemsError.OpenemsNamedException, ConfigurationException {
        if (cpm.getComponent(device) instanceof Thermometer) {
            Thermometer th = cpm.getComponent(device);
            switch (concreteType) {
                case CONCRETE_TYPE_TEMPERATURE_SENSOR_REFERENCE:
                    return th;

                default:
                    throw new ConfigurationException("This exception shouldn't occur somethings wrong with identifier", "identifier wrong");
            }
        } else {
            throw new ConfigurationException("The Device " + device
                    + " is not a TemperatureSensor", "Configuration is wrong of TemperatureSensor");
        }

    }

    private boolean checkTimeIsUp(DateTime timeStamp, int seconds) {
        if (timeStamp == null) {
            return true;
        }
        DateTime now = new DateTime();
        DateTime compare = new DateTime(timeStamp).plusSeconds(seconds);
        return now.isAfter(compare);
    }


    private void startHeating() {
        try {
            if (this.lineHeater.startHeating()) {
                this.isRunning().setNextValue(true);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to heat!");
        }

    }

    private void stopHeating(DateTime lifecycle) {
        try {
            if (this.lineHeater.stopHeating(lifecycle)) {
                this.isRunning().setNextValue(false);
            }
        } catch (OpenemsError.OpenemsNamedException e) {
            this.log.error("Error while trying to stop Heating");
        }
    }

    private boolean getHeatRequestHeater() {
        if (this.decentralHeaterOptional != null) {
            return this.decentralHeaterOptional.getNeedHeat();
        } else {
            return false;
        }
    }

    @Override
    public void run() throws OpenemsError.OpenemsNamedException {
        int tempReference = this.tempSensorReference.getTemperatureChannel().value().isDefined() ? this.tempSensorReference.getTemperatureChannel().value().get() : DEFAULT_TEMPERATURE;
        DateTime now = new DateTime();
        boolean decentralHeatRequest = this.getHeatRequestHeater();
        Optional<Boolean> signal = this.enableSignal().getNextWriteValueAndReset();
        boolean missingEnableSignal = signal.isPresent() == false;
        boolean enableSignal = signal.isPresent() ? signal.get() : false;

        if (missingEnableSignal && this.shouldFallback && checkTimeIsUp(this.initalTimeStampFallback, this.timeoutMaxRemote)) {
            this.isFallback = true;
            this.isFallback().setNextValue(true);

        } else {
            this.initalTimeStampFallback = now;
            this.isFallback = false;
            this.isFallback().setNextValue(false);
        }

        if (this.isFallback) {
            if (this.isMinuteFallbackStart(now.getMinuteOfHour())) {
                this.startHeating();
            } else {
                this.stopHeating(now);
            }
        }
        if (decentralHeatRequest || enableSignal) {

            if (tempReference < this.temperatureDefault) {
                if (this.checkTimeIsUp(this.lineHeater.getLifeCycle(), this.timeoutRestartCycle)) {
                    this.startHeating();
                }
            } else {
                //temperature Reached
                this.stopHeating(now);
            }
        }
    }

    private boolean isMinuteFallbackStart(int minuteOfHour) {
        boolean isStartAfterStop = this.minuteFallbackStart > this.minuteFallbackStop;
        if (isStartAfterStop) {
            //if start = 45 and stop 15
            //logic switches start from 00-15 or from 45-60
            if (minuteOfHour >= this.minuteFallbackStart || minuteOfHour < this.minuteFallbackStop) {
                return true;
            } else {
                return false;
            }
        } else {

            if (minuteOfHour >= this.minuteFallbackStart && minuteOfHour < this.minuteFallbackStop) {
                return true;
            } else {
                return false;
            }
        }

    }

    @Override
    public String debugLog() {
        return null;
    }

    @Deactivate
    public void deactivate() {
        super.deactivate();
    }
}
