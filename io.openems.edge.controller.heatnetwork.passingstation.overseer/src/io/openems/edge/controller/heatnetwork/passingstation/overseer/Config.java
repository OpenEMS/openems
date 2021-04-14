package io.openems.edge.controller.heatnetwork.passingstation.overseer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Overseer",
        description = "This Controller regulates the Pump and Valves for Heating."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Passing Controller.")
    String id() default "ControllerOverseer0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "";

    @AttributeDefinition(name = "Allocated Passing Controller", description = "Unique Name of the Passing Controller, allocated with this Overseer.")
    String allocated_Passing_Controller() default "ControllerPassing0";

    @AttributeDefinition(name = "Temperature", description = "The Temperature you want to reach (T in dC--> 1°C = 10).")
    int min_Temperature() default 750;

    @AttributeDefinition(name = "Temperature Range", description = "The tolerated Temperature Range in dC.")
    int tolerated_Temperature_Range () default 60;

    @AttributeDefinition(name = "Temperature Sensor", description = "The Temperature Sensor which is allocated with this Controller.")
            String [] allocated_Temperature_Sensor() default {"TemperatureSensor4"};

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Overseer [{id}]";
}