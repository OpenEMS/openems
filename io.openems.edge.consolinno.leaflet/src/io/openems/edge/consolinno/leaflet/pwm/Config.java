package io.openems.edge.consolinno.leaflet.pwm;


import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Pwm", description = "Pwm that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Pwm.")
    String id() default "Pwm0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Sensor.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Sensor is plugged in.")
    int module();

    @AttributeDefinition(name = "Position", description = "Pinposition of this Pwm.")
    int position();

    @AttributeDefinition(name = "Duty Cycle", description = "Pwm Output in  Percent (100=10.0%).")
    int percent() default 0;

    @AttributeDefinition(name = "Inverted", description = "Tick if Pwm is supposed to be inverted.")
    boolean isInverted() default false;

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Core.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Core.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "Pwm [{id}]";
}
