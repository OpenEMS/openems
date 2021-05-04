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

    @AttributeDefinition(name = "MaxIntervalToWait", description = "To prevent fluctuations in Temperature (jumping from 500dC to 490dC immediately) you can set an intervalcounter")
    int maxInterval() default 1;

    boolean useAnotherChannelAsTemperature() default true;

    @AttributeDefinition(name = "Other Channel", description = "If you want to use another Channel as a Temperature write the ChannelAddress here (Useful if Ref. Temperature is in a Modbus device)")
    String channelAddress() default "AM_1/LastKnownTemperature";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Thermometer Virtual [{id}]";
}