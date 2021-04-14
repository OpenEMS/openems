package io.openems.edge.controller.heatnetwork.valvepumpcontrol;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno Valve and Pump hierarchy Controller",
        description = "Control module to manage different controllers manipulating the same valve and pump."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the valve and pump control.")
    String id() default "ValveAndPumpControl0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Zugriffs Controller für Ventil und Pumpe";

    @AttributeDefinition(name = "Allocated Heating Controller", description = "Unique Name of the heating controller, allocated to this component.")
    String allocated_Heating_Controller() default "ControllerPassingControlCenter0";

    @AttributeDefinition(name = "heat network valve US01", description = "The valve opening the connection to the heat network.")
    String valveUS01Id() default "US01";

    @AttributeDefinition(name = "heating circuit pump HK01", description = "The pump operating the heating circuit HK01.")
    String pumpHK01Id() default "HK01";

    @AttributeDefinition(name = "minimumClosingValve", description = "Closing the US01 to minimum percent")
    int closeValvePercent() default 20;

    @AttributeDefinition(name = "TemperatureSensorFLow", description = "The Temperaturesensor in Flow direction")
    String temperatureSensorFlow() default "";
    @AttributeDefinition(name = "TemperatureSensorReturn", description = "The Temperaturesensor in Return direction")
    String temperatureSensorReturn() default "";

    @AttributeDefinition(name = "timeToResetSeconds", description = "if no Temperaturedifference over this time reset valve")
    int timeToResetSeconds() default 60;

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno Valve and Pump Control [{id}]";
}