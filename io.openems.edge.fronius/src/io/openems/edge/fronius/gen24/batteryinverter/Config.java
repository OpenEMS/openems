package io.openems.edge.fronius.gen24.batteryinverter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS Fronius Gen24 Hybrid", //
		description = "Implements the Fronius Gen24 hybrid inverter."
				+ "ONLY WORKS with MODBUS Server ENABLED AND INT+SF Setting.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "batteryInverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus-Unit-ID", description = "Modbus Unit-ID (Unit-ID).")
	int modbusUnitId() default 1;

	@AttributeDefinition(name = "Control mode", description = "Sets the Control mode. \"Internal\" allows no power setpoints, "
			+ "\"Remote\" allows full control by OpenEMS")
	ControlMode controlMode() default ControlMode.INTERNAL;

	String webconsole_configurationFactory_nameHint() default "ESS Fronius Gen24 Hybrid [{id}]";
}
