package io.openems.edge.thermometer.virtual;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Thermometer Virtual",
        description = "Thermometer holding a Virtual Temperature"
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "TemperatureThreshold Id", description = "ID of the Temperature Threshold")
    String id() default "ThermometerVirtual0";

    @AttributeDefinition(name = "Alias", description = "Human readable Name.")
    String alias() default "";

    @AttributeDefinition(name = "MaxIntervalToWait", description = "To prevent fluctuations in Temperature (jumping from 500dC to 490dC immediately) you can set an intervallcounter")
    int maxInterval() default 1;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Thermometer Virtual [{id}]";
}