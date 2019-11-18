package io.openems.edge.pwm;
import com.sun.corba.se.spi.ior.IdentifiableFactory;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(
        name = "Consolinno PwmModule",
        description = "Module for Pulse widening modulation"
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Pwm Name", description = "")
    String id() default "Pwm0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    @AttributeDefinition(name = "Version", description = "What Version of Consolinno Pwm Module are you using.")
    String version() default "1";

    @AttributeDefinition(name = "I2C Bridge - ID", description = "ID of I2C Bridge - ID.")
    String spiI2c_id() default "I2C0";

    @AttributeDefinition(name = "Bus Device Address", description = "What I2C Bus are you using.")
    short bus_address() default 1;


    @AttributeDefinition(name = "Device Address", description = "The address of your Pwm Module")
    String pwm_address() default "0x55";

    @AttributeDefinition(name = "Steps in microseconds", description = "Resolution of x microseconds per step")
            int step_Micro() default 5;

    @AttributeDefinition(name = "Measured Frequency", description = "What Frequency are you actualle Measuring")
            String actual_frequency() default "51.68";

    @AttributeDefinition(name = "offset", description = "Offset for Pwm Module.")
            int pwm_offset() default 400;

    @AttributeDefinition(name = "pulseDuration", description = "pulseDuration of Pwm Module")
            int pwm_pulseDuration()default 600;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Pwm Module [{id}]";
}
