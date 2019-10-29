package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Temperature Sensor", description = "TemperatureSensors for the PT1000_16IN Module")
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Temperature Sensor")
    String id() default "TemperatureSensor0";

    @AttributeDefinition(name = "CircuitBoardId", description = "Same Id as CircuitBoard connected to it")
    String circuitBoardId() default "Temperature0";

    @AttributeDefinition(name = "Spi Channel", description = "What Spi Channel the configured Sensor uses (look at dipSwitch)")
    short spiChannel() default (short) 0;

    @AttributeDefinition(name = "Pin Position", description = "What Pin Position of the Adc you want to use, starting with No. 0")
    byte pinPosition() default (byte) 0;

    @AttributeDefinition(name = "SPI-Initial Id", description = "Id of SPI initial.")
    String spiInitial_id() default "spi0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor")
    String alias() default "";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "TemperatureSensor [{id}]";
}
