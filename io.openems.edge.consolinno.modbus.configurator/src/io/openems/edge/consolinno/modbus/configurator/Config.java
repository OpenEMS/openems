package io.openems.edge.consolinno.modbus.configurator;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "Consolinno Leaflet Modbusconfigurator", description = "Configurator for Modbus Leafletmodules.")
@interface Config {

    @AttributeDefinition(name = "Id", description = "Unique Id for the Module Configurator.")
    String id() default "Configurator";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this Configurator.")
    String alias() default "";

    @AttributeDefinition(name = "Source", description = "Path of the Sourcefile")
    String source() default "/usr/include/LeafletModbus.csv";

    boolean enabled() default true;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId();
    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge")
    String modbusBridgeId();
    String webconsole_configurationFactory_nameHint() default "{id}";
}
