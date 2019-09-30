package io.openems.edge.raspberrypi.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name= "Analogue Sensors",
description = "Abstract Class for analogue Sensors communicating with raspberry Pi.")
@interface Config {
@AttributeDefinition(name = "SensorID", description = "Unique ID of SensorComponent")
    String SensorID() default "Sensor0";
@AttributeDefinition(name = "Chip ID's", description = "Which ADC ID's u want to implement, seperate via ';'")
    String ChipID() default "Chip0";
@AttributeDefinition(name="BoardID's", description = "What Board(s) used for each ADC, seperate with ';'")
String BoardID() default "LEAFLET_1_00";
@AttributeDefinition(name="ADCTypes", description = "What ADCs did you use, seperate with ';'")
    String ADCTypes() default "MCP3208";
@AttributeDefinition(name="PinUsage", description = "What Pins you want to use for which ADC: Grouped Pins via ',' Different Chip: ';'")
    String PinUsage()default "0-8";

}
