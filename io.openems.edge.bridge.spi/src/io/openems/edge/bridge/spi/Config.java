package io.openems.edge.bridge.spi;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
        name = "Bridge SPI", //
        description = "Provides a service for connecting to, reading and writing an SPI device.")
@interface Config {
    String service_pid();

    String id() default "spi0";

    @AttributeDefinition(name = "Frequency", description = "The frequency of the SPI Numbers Between 500_000 and 32_000_000.")
    int frequency() default 500_000;
    int cs_0() default 0;
    int cs_1() default 1;

    @AttributeDefinition(name = "SPI", description= "Test")
    int spi() default 19;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bridge SPI [{id}]";
}