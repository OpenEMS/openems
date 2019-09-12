package io.openems.edge.goodwe.et.charger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "GoodWe ET Charger PV2", //
		description = "Implements the Goodwe-ET Charger.")

public @interface ConfigPV2 {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger1";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Modbus Unit-id", description = "Unit-id")
	int unit_id() default 0xF7;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	String webconsole_configurationFactory_nameHint() default "GoodWe ET Charger PV2 [{id}]";
}