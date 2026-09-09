package io.openems.edge.goodwe.emergencypowermeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.goodwe.GoodWeConstants;

@ObjectClassDefinition(//
		name = "GoodWe EmergencyPowerMeter", //
		description = "GoodWe Emergency Power Meter.")

@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter2";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus1";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default GoodWeConstants.DEFAULT_UNIT_ID;

	String webconsole_configurationFactory_nameHint() default "GoodWe EmergencyPowerMeter [{id}]";
}
