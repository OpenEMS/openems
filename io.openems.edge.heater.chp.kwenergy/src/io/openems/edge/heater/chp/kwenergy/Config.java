package io.openems.edge.heater.chp.kwenergy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Chp KW Energy Smartblock",
        description = "A module to map Modbus calls to OpenEMS channels for a KW Energy Smartblock CHP."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "CHP-Device ID", description = "Unique Id of the CHP.")
    String id() default "Chp0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of CHP.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Default set point for control mode \"power percent\"", description = "If the CHP is in control mode \"power percent\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 100, unit is percent. So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 70;

    @AttributeDefinition(name = "Default set point for control mode \"electric power\"", description = "If the CHP is in control mode \"electric power\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 22, unit is kilowatt.")
    int defaultSetPointElectricPower() default 18;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode.")
    boolean debug() default false;


    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "CHP KW Energy Smartblock [{id}]";

}