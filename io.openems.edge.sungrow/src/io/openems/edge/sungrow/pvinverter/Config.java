package io.openems.edge.sungrow.pvinverter;

import io.openems.common.types.MeterType;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
	name = "PV-Inverter Sungrow", //
	description = "Implements the Sungrow PV inverter")
@interface Config {

    @AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
    String id() default "pvInverter0";

    @AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
    String alias() default "";

    @AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
    boolean enabled() default true;
        
    @AttributeDefinition(name = "Read-Only mode", description = "Set to true, if PV Inverter should only be read.")
    boolean readOnly() default true;

    @AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
    String modbus_id() default "modbus0";

    @AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
    int modbusUnitId() default 1;

    @AttributeDefinition(name = "Meter-Type", description = "Always Production")
    MeterType type() default MeterType.PRODUCTION;

    String webconsole_configurationFactory_nameHint() default "PV-Inverter Sungrow [{id}]";

}