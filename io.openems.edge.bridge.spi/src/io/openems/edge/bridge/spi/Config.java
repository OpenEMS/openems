package io.openems.edge.bridge.spi;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
        name = "Bridge SPI", //
        description = "Provides a service for connecting to, reading and writing an SPI device.")
@interface Config {
    String service_pid();

    String id() default "spi0";

    @AttributeDefinition(name = "Frequency", description = "The frequency of the SPI.")
    int frequency() default 500_000;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Bridge SPI [{id}]";
}