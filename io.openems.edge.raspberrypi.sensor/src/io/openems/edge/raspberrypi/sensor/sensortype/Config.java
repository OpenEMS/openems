package io.openems.edge.raspberrypi.sensor.sensortype;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition
( //
        name = "SensorType", //
        description = "This is the Subtype of your ParentSensor; e.g. if your parent Sensor can track multiple Stuff, here you go")

@interface Config {

   @AttributeDefinition(name="SensorTypeID",
                        description = "Subtype of your previous activated Sensor Like:")
    String id() default "Sensor0";
   @AttributeDefinition(name = "ChannelId", description = "What the SensorType is used for/Which Channel it should write")
   String channelId() default "Temperature";

}
