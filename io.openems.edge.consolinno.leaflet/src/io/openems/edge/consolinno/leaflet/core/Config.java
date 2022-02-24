package io.openems.edge.consolinno.leaflet.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Core", description = "Core for Modbus LeafletModules. "
        + "This must be activated first, before any LeafletModules can be used.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Leaflet Core.")
    String id() default "LeafletCore";

    @AttributeDefinition(name = "Alias", description = "Human readable name of the Core.")
    String alias() default "";

    @AttributeDefinition(name = "Source", description = "Path of the SourceFile where the Modbus-registerMap is stored.")
    String source() default "/usr/include/leafletbs/modbusregmap.csv";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the ModbusUnit.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the ModbusBridge")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "LeafletCore {id}";
}
