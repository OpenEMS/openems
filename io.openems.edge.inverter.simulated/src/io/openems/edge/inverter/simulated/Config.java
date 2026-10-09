package io.openems.edge.inverter.simulated;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(
        name = "Inverter (PPC PV Plant)",
        description = "PPC PV plant"
)
@interface InverterConfig {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this component.")
    String id() default "pvInverter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name.")
    String alias() default "PPC PV Plant Inverter";

    @AttributeDefinition(name = "Is enabled?")
    boolean enabled() default true;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of the Modbus bridge (BridgeModbusTcp).")
    String modbus_id() default "modbus1";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "Modbus Unit-ID of the Node-RED slave (default 1).")
    int modbusUnitId() default 1;

    String webconsole_configurationFactory_nameHint() default "Inverter (PPC PV Plant) [{id}]";
}