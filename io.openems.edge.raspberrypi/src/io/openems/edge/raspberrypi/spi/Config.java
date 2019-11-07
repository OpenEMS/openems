package io.openems.edge.raspberrypi.spi;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Spi Initial",
        description = "Initial Spi, opens Ch 0 and handles Events etc"
)

@interface Config {
    String service_pid();
    @AttributeDefinition(name = "SpiInitial", description = "First thing you need to Config, no further SpiInitials needed. Continue with CircuitBoard.")
    String id() default "spi0";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
            String alias() default "";
    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Initial SPI [{id}]";
}
