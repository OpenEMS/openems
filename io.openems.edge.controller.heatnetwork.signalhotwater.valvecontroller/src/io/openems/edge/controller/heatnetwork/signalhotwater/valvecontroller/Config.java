package io.openems.edge.controller.heatnetwork.signalhotwater.valvecontroller;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Controller Consolinno SignalHotWater Valvecontroller",
        description = "Controller that switches the valves according to the \"need hot water\" signal."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Controller Name", description = "Unique Name of the Heating Controller.")
    String id() default "SignalHotWaterValvecontroller0";

    @AttributeDefinition(name = "alias", description = "Human Readable Name of Component.")
    String alias() default "Warmwasser Anforderung Ventilsteuerung";

    @AttributeDefinition(name = "SignalHotWater Controller", description = "Name of the SignalHotWater controller that sends commands to this controller.")
    String signalHotWaterId() default "SignalHotWater0";

    @AttributeDefinition(name = "Heat exchanger forward temperature sensor", description = "The temperature sensor of the heat exchanger hot water input pipe.")
    String temperatureSensorVlId() default "T_US_VL";

    @AttributeDefinition(name = "Minimum temperature forward sensor", description = "Minimum temperature of the heat exchanger hot water input pipe. Unit is °C")
    int min_temp_vl() default 65;

    @AttributeDefinition(name = "Valve timeout", description = "Time in minutes to wait for reaction before the valve is considered defunct.")
    int timeout() default 5;

    @AttributeDefinition(name = "Valve Opening", description = "How much the valve should be opened")
    int getValvePercent() default 80;


    @AttributeDefinition(name = "overrider module for valve US01", description = "The module that handles override access to valve US01.")
    String valveUS01overrideId() default "ValveAndPumpControl0";

    @AttributeDefinition(name = "Water tank heating forward valve", description = "The input valve of the water tank heater.")
    String valveTL01Id() default "TL01";

    boolean enabled() default true;

    String webconsole_configurationFactory_nameHint() default "Controller Consolinno SignalHotWater Valvecontroller[{id}]";
}