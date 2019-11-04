package io.openems.edge.bridgei2c;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Bridge I2C",
        description = "Bridge to use the connected Relais"
)

@interface Config {
    String service_pid();


    boolean enabled() default true;

    @AttributeDefinition(name = "I2CBridge-ID", description = "ID of Shiftregister brige.")
    String id() default "I2C0";


    @AttributeDefinition(name = "Position", description = "The position. Starting by 0")
    int position();

    @AttributeDefinition(name = "IsOpener", description = "Is the relais an opener or closer")
    boolean isOpener();

    String webconsole_configurationFactory_nameHint() default "Actuator Relais Shiftregister[{id}]";
}