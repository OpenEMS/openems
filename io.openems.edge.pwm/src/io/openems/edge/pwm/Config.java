package io.openems.edge.pwm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno Pwm Module",
        description = "Module for Pulse widening modulation"
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Pwm Module Name", description = "The Unique Id of the Module")
    String id() default "Pwm0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Version", description = "What Version of Consolinno Pwm Module are you using.",
    options = @Option(label = "Version 1.0", value = "1"))
    String version() default "1";

    @AttributeDefinition(name = "Bus Device Address", description = "What I2C Bus are you using.")
    short bus_address() default 1;

    @AttributeDefinition(name = "Device Address", description = "The address of your Pwm Module.")
    String pwm_address() default "0x55";

    @AttributeDefinition(name = "Allocated Frequency", description = "How much Hz supported by the Module (Max 1526).")
            String max_frequency() default "500";

    @AttributeDefinition(name = "Measured Frequency", description = "actual measured Frequency of Module.")
            String actual_frequency() default "500";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Pwm Module [{id}]";
}
