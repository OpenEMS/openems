package io.openems.edge.consolinno.leaflet.relay;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbus Relay", description = "One Relay on a Leaflet Relays Module that communicates over Modbus.")
@interface Config {
    @AttributeDefinition(name = "Id", description = "Unique Id for this Relay.")
    String id() default "Relay0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Relay.")
    String alias() default "";

    @AttributeDefinition(name = "Module", description = "ModuleNumber where this Relay is plugged in.")
    int module() default 1;

    @AttributeDefinition(name = "Position", description = "PinPosition of this Relay.")
    int position() default 1;

    @AttributeDefinition(name = "Inverse", description = "Tick if this Relay is Normally Closed")
    boolean isInverse() default false;

    @AttributeDefinition(name = "LeafletId", description = "Unique Id of the LeafletCore, this Module is attached to.")
    String leafletId() default "LeafletCore";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "ModbusUnitId from Core.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "ModbusBridgeId from Core.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "Relay [{id}]";
}
