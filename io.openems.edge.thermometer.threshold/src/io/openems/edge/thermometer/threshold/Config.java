package io.openems.edge.thermometer.threshold;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Thermometer Threshold",
        description = "Thermometer Threshold holding a Thermometer, Threshold, and a Interval"
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "TemperatureThreshold Id", description = "ID of the Temperature Threshold")
    String id() default "ThermometerThreshold0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";

    @AttributeDefinition(name = "ThermometerId", description = "Thermometer to watch")
    String thermometerId() default "TemperatureSensor0";

    @AttributeDefinition(name = "thresholdTemperature", description = "The Threshold of this component (e.g. 10dC means only check 480 490 500 dc etc)")
    int thresholdTemperature() default 10;

    @AttributeDefinition(name = "MaxIntervalToWait", description = "To prevent fluctuations in Temperature (jumping from 500dC to 490dC immediately) you can set an intervalCounter")
    int maxInterval() default 1;

    @AttributeDefinition(name = "Starting SetPoint Temperature", description = "Default/Start value of the Setpoint Temperature, that will be set at init.; Unit: DeciDegree")
    int startSetPointTemperature() default 500;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "TemperatureThreshold [{id}]";
}