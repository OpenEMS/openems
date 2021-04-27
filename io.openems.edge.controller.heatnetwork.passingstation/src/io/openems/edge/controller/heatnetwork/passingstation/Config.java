package io.openems.edge.controller.heatnetwork.passingstation;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Passing",
        description = "This Controller regulates the Pump and Valves for Heating."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerPassing0";

    @AttributeDefinition(name = "alias", description = "Human readable name of Controller.")
    String alias() default "Uebergabestation";

    @AttributeDefinition(name = "Primary Forward Temperature Sensor Id", description = "The TemperatureSensor Id used to measure the Primary Forward Temperature.")
    String primary_Forward_Sensor() default "TemperatureSensor0";

    @AttributeDefinition(name = "Primary Rewind Temperature Sensor Id", description = "The TemperatureSensor Id used to measure the Primary Rewind Temperature.")
    String primary_Rewind_Sensor() default "TemperatureSensor1";

    @AttributeDefinition(name = "Secundary Forward Temperature Sensor Id", description = "The TemperatureSensor Id used to measure the Secundary Forward Temperature.")
    String secundary_Forward_Sensor() default "TemperatureSensor2";

    @AttributeDefinition(name = "Secundary Rewind Temperature Sensor Id", description = "The TemperatureSensor Id used to measure the Secundary Rewind Temperature.")
    String secundary_Rewind_Sensor() default "TemperatureSensor3";

    @AttributeDefinition(name = "Valve Id", description = "The Unique Valve Id allocated with the passing controller.")
    String valve_id() default "Valve0";

    @AttributeDefinition(name = "Pump Id", description = "The Unique Pump Id allocated with the passing controller.")
    String pump_id() default "Pump0";

    @AttributeDefinition(name = "Heating Time", description = "The Time needed to heat up the Primary Forward (t in seconds).")
    int heating_Time() default 500;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Passing [{id}]";

}

