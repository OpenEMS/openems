package io.openems.edge.raspberrypi.spi;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name= "SpiInitial",
        description = "Initial Spi, opens Ch 0 and handles Events etc"
)

@interface Config {
<<<<<<< HEAD
    String service_pid();
=======
>>>>>>> SPI

    @AttributeDefinition(name = "SpiInitial", description = "Opens Spi Channel for Rasperry Pi")
    String id() default "spi0";
    @AttributeDefinition(name="Frequency", description = "Default Frequency of Devices Connected to Pi")
    int frequency() default 500_000;

<<<<<<< HEAD
    boolean enabled() default true;

=======
>>>>>>> SPI
    String webconsole_configurationFactory_nameHint() default "Bridge SPI [{id}]";
}
