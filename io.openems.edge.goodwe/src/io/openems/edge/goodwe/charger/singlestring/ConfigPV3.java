package io.openems.edge.goodwe.charger.singlestring;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.goodwe.GoodWeConstants;

@ObjectClassDefinition(//
		name = "GoodWe Charger PV3", //
		description = "Implements the GoodWe-ET Charger 3.")

@interface ConfigPV3 {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger2";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "GoodWe ESS or Battery-Inverter", description = "ID of GoodWe Energy Storage System or Battery-Inverter.")
	String essOrBatteryInverter_id() default "batteryInverter0";

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default GoodWeConstants.DEFAULT_UNIT_ID;

	String webconsole_configurationFactory_nameHint() default "GoodWe Charger PV3 [{id}]";
}
