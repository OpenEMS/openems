package io.openems.edge.evcs.wallbe;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;


@ObjectClassDefinition(name = "Evcs Wallbe BETA", description = "Implements the Wallbe electric vehicle charging station.")
@interface Config {

    @AttributeDefinition(name = "Component-Id", description = "Unique Id for the EVCS.")
    String id() default "evcs0";

    @AttributeDefinition(name = "Alias", description = "Human readable name of this EVCS.")
    String alias() default "";

    boolean enabled() default true;

    @AttributeDefinition(name = "Minimum Current", description = "Minimum current of the Charger in mA.", required = true)
    int minHwCurrent() default 6000;

    @AttributeDefinition(name = "Maximum Current", description = "Maximum current of the Charger in mA.", required = true)
    int maxHwCurrent() default 32000;

    @AttributeDefinition(name = "ModbusUnitId", description = "Unique Id for the Modbusunit.")
    int modbusUnitId() default 255;

    @AttributeDefinition(name = "ModbusBridgeId", description = "Unique Id for the Modbusbridge.")
    String modbusBridgeId() default "modbus0";

    String webconsole_configurationFactory_nameHint() default "Evcs Wallbe [{id}]";
}
