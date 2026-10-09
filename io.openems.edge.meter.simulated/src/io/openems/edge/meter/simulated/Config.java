package io.openems.edge.meter.simulated;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(
        name = "Meter (PPC Grid)",
        description = "Grid Meter reading PPC parameters"
)
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this component.")
    String id() default "meter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name for this component.")
    String alias() default "PPC Grid Meter";

    @AttributeDefinition(name = "Is enabled?", description = "Is this component enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Meter Type", description = "Type of meter (GRID, PRODUCTION, CONSUMPTION, etc.).")
    MeterType type() default MeterType.GRID;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of the Modbus bridge component (BridgeModbusTcp).")
    String modbus_id() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Modbus Unit-ID of the Node-RED slave (default 1).")
    int modbusUnitId() default 1;

    String webconsole_configurationFactory_nameHint() default "Meter (PPC Grid) [{id}]";
}