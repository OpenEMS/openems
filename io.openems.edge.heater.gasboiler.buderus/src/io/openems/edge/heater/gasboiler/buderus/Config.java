package io.openems.edge.heater.gasboiler.buderus;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Heater Buderus",
        description = "A module to map Modbus calls to OpenEMS channels for a Buderus heater."
)
@interface Config {

    String service_pid();

    @AttributeDefinition(name = "Heater-Device ID", description = "Unique Id of the heater.")
    String id() default "Heater0";

    @AttributeDefinition(name = "ModBus-Bridge Id", description = "The Unique Id of the modBus-Bridge you what to allocate to this device.")
    String modbusBridgeId() default "modbus0";

    @AttributeDefinition(name = "alias", description = "Human readable name of heater.")
    String alias() default "";

    @AttributeDefinition(name = "ModBus-Unit Id", description = "Integer Unit Id of the Component.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Default set point for control mode \"power percent\"", description = "If the heater is in control mode \"power percent\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 100, unit is percent. So 50 means 50% of maximum power.")
    int defaultSetPointPowerPercent() default 100;

    @AttributeDefinition(name = "Default set point for control mode \"temperature\"", description = "If the heater is in control mode \"temperature\" and receives the command to turn on, this value will be used if no set point is received. Valid values are 0 to 120, unit is °C.")
    int defaultSetPointTemperature() default 100;

    @AttributeDefinition(name = "Read only", description = "Only read values from Modbus, don't send commands.")
    boolean readOnly() default false;

    @AttributeDefinition(name = "Debug", description = "Enable debug mode.")
    boolean debug() default false;


    boolean enabled() default true;


    String webconsole_configurationFactory_nameHint() default "Heater Buderus Device [{id}]";

}