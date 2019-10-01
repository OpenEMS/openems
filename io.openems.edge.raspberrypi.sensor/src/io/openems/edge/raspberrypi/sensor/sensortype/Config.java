package io.openems.edge.raspberrypi.sensor.sensortype;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
( //
        name = "SensorType", //
        description = "This is the Subtype of your ParentSensor; " +
                        "e.g. if your parent Sensor can track multiple Stuff, here you go")

@interface Config {

   @AttributeDefinition(name="SensorTypeID",
                        description = "Subtype of your previous activated Sensor Like: Temperature0")
    String typeId() default "child0";

   @AttributeDefinition(name = "SensorId", description = "Device Name, what you previously activated, " +
                                                            "NOTE!: this must be the exact same name")
    String fatherId() default "Sensor0";
   @AttributeDefinition(name="ADC_Ids", description = "What Adcs u want to use: Identified via Id, seperated via ';', at least one ';'")
    String adcId() default "0;";

   @AttributeDefinition(name="Pins to use", description = "Like in Sensor, type in what Pins u want to use seperate section with ;")
    String pinPositions() default "01234567";
   @AttributeDefinition(name="OpenEmsChannel", description = "What this Type can measure. Like: Temperature")
    String channelId() default "Temperature";
}
