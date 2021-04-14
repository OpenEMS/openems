package io.openems.edge.consolinno.relay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Modbus Relay", description = "Relay that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Relay.")
    String id() default "Relay0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Relay.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Relay is plugged in.")
    int module();

    @AttributeDefinition(name = "Position", description = "Pinposition of this Relay.")
    int position();

    @AttributeDefinition(name = "Inverse", description = "Tick if this Relay is Normaly Closed")
    boolean isInverse() default false;

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Configurator.")
    int modbusUnitId();

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Configurator.")
    String modbusBridgeId();

    String webconsole_configurationFactory_nameHint() default "Relay [{id}]";
}
