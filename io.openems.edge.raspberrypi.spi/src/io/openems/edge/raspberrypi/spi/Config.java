package io.openems.edge.raspberrypi.spi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name= "SpiInitial",
        description = "Initial Spi, opens Ch 0 and handles Events etc"
)

@interface Config {
    String service_pid();

    @AttributeDefinition(name = "SpiInitial", description = "Opens Spi Channel for Rasperry Pi")
    String id() default "spi0";
    @AttributeDefinition(name="Frequency", description = "Default Frequency of Devices Connected to Pi")
    int frequency() default 500_000;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bridge SPI [{id}]";
}
