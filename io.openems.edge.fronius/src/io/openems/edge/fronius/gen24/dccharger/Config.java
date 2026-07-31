package io.openems.edge.fronius.gen24.dccharger;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.fronius.enums.PvString;

@ObjectClassDefinition(//
		name = "ESS Fronius Gen24 DC Charger PV", //
		description = "Implements the chosen Channel of the Gen24 as PV Inverter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "charger0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge. Must point at the same physical Fronius Gen24 device as the BatteryInverter's Modbus configuration.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus-Unit-ID", description = "Modbus Unit-ID. Must match the BatteryInverter's Unit-ID, since both talk to the same physical device.")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "String Number", description = "Fronius Gen24 String to use.")
	PvString pvString() default PvString.ONE;

	String webconsole_configurationFactory_nameHint() default "ESS Fronius Gen24 DC Charger PV [{id}]";

}
