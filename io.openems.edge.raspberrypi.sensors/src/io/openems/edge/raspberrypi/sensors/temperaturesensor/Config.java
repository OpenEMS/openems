package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TemperatureSensor", description = "TemperatureSensors for the PT1000_16IN Module")
 @interface Config {
   String service_pid();
    @AttributeDefinition(name = "Id", description = "Unique Id for this Temperature Sensor")
    String sensorId() default "TemperatureSensor0";

    @AttributeDefinition(name = "CircuitBoardId", description = "Same Id as CircuitBoard connected to it")
    String circuitBoardId() default "Temperature0";

    @AttributeDefinition(name = "Adc number", description = "What Adc Number on Board u want to use, starting with No. 0")
    short adcNumber() default (short) 0;

    @AttributeDefinition(name = "Pin Position", description = "What Pin Position of the Adc you want to use, starting with No. 0")
    byte pinPosition() default (byte) 0;
    @AttributeDefinition(name = "SPI-Initial Id", description = "Id of SPI initial.")
            String spi_id() default "spi0";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "TemperatureSensor [{id}]";
}
