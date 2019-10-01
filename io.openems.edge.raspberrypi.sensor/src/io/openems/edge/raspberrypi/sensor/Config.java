package io.openems.edge.raspberrypi.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name= "Analogue Sensors",
description = "Abstract Class for analogue Sensors communicating with raspberry Pi.")
@interface Config {
@AttributeDefinition(name = "SensorId", description = "Unique ID of SensorComponent")
    String sensorId() default "Sensor0";
@AttributeDefinition(name = "Adc ID's", description = "Which ADC ID's u want to implement, seperate via ';'")
    String adcId() default "0";
@AttributeDefinition(name="BoardID's", description = "What Board(s) used for each ADC, seperate with ';'")
String boardId() default "LEAFLET_1_00";
@AttributeDefinition(name="ADCTypes", description = "What ADCs did you use, seperate with ';'")
    String adcType() default "MCP3208";
@AttributeDefinition(name="PinUsage", description = "What Pins you want to use for which ADC: Grouped Pins via ',' or '-' Different Chip: ';'")
    String pinUsage()default "0-8";

}
