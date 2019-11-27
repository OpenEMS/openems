package io.openems.edge.temperature.sensor;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Temperature Sensor", description = "Temperature Sensors for the Consolinno Temperature Module.")
@interface Config {
    String service_pid();

    @AttributeDefinition(name = "Id", description = "Unique Id for this Temperature Sensor.")
    String id() default "TemperatureSensor0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Temperature Board Id", description = "Same Id as CircuitBoard connected to it.")
    String temperatureBoardId() default "TemperatureBoard0";

    @AttributeDefinition(name = "Dip Switch", description = "What Dip switch is used for this Temperature Sensor (Starting with 0)")
    short spiChannel() default (short) 0;

    @AttributeDefinition(name = "Pin Position", description = "What Pin Position of the Adc you want to use, starting with No. 0")
    short pinPosition() default 0;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "TemperatureSensor [{id}]";
}
