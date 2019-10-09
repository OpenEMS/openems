package io.openems.edge.raspberrypi.sensors.temperaturesensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "TemperatureSensor", description = "TemperatureSensors for the PT1000_16IN Module")
 @interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Temperature Sensor")
    String sensorId() default "sensor0";

    @AttributeDefinition(name = "CircuitBoardId", description = "Same Id as CircuitBoard connected to it")
    String circuitBoardId() default "Temperature0";

    @AttributeDefinition(name = "Adc number", description = "What Adc Number on Board u want to use")
    short adcNumber() default (short) 0;

    @AttributeDefinition(name = "Pin Position", description = "What Pin Position of the Adc you want to use")
    byte pinPosition() default (byte) 0;
}
