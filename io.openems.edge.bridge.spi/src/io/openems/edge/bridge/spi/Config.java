package io.openems.edge.bridge.spi;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Bridge Spi",
        description = "Initial Spi, needed for Consolinno Temperature Board and Sensor."
)
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "SpiInitial", description = "First thing you need to Config, no further SpiInitials needed. Continue with CircuitBoard.")
    String id() default "Spi";

    @AttributeDefinition(name = "Alias", description = "Human readable name for this Component.")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Initial SPI [{id}]";
}
