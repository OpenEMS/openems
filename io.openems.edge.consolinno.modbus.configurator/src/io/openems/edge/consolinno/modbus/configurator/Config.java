package io.openems.edge.consolinno.modbus.configurator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet ModbusConfigurator", description = "Configurator for Modbus LeafletModules.")

@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Module Configurator.")
    String id() default "Configurator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Source", description = "Path of the SourceFile")
    String source() default "/usr/include/leaflet/modbusregmap.csv";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the ModbusUnit.")
    int modbusUnitId();
    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the ModbusBridge")

    String modbusBridgeId();
    String webconsole_configurationFactory_nameHint() default "{id}";
}
