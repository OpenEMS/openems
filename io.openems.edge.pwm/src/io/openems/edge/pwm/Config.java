package io.openems.edge.pwm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Consolinno Pwm Module",
        description = "Module for Pulse widening modulation"
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Pwm Module Name", description = "")
    String id() default "Pwm0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Version", description = "What Version of Consolinno Pwm Module are you using.")
    String version() default "1";

    @AttributeDefinition(name = "I2C Bridge - ID", description = "ID of I2C Bridge - ID.")
    String i2c_id() default "I2C0";

    @AttributeDefinition(name = "Bus Device Address", description = "What I2C Bus are you using.")
    short bus_address() default 1;

    @AttributeDefinition(name = "Device Address", description = "The address of your Pwm Module")
    String pwm_address() default "0x55";

    @AttributeDefinition(name = "Maximum Hz", description = "Maximum Hz supported by the Module")
            String max_frequency() default "1526";

    @AttributeDefinition(name = "Measured Frequency", description = "actual measured Frequency of Module")
            String actual_frequency() default "1526";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Pwm Module [{id}]";
}
