package io.openems.edge.kostal.plenticore.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.kostal.plenticore.enums.ControlMode;

@ObjectClassDefinition(//
		name = "KOSTAL Plenticore ESS", //
		description = "Implements the Kostal Plenticore hybrid energy storage system (battery).")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Read-Only mode", description = "In Read-Only mode no set-power-commands are sent to the inverter")
	boolean readOnlyMode() default true;

	@AttributeDefinition(name = "Control mode", description = "Sets the Control mode")
	ControlMode controlMode() default ControlMode.INTERNAL;

	@AttributeDefinition(name = "Minimum Battery-Soc", description = "The minimum battery state of charge.")
	int minsoc() default 5;

	@AttributeDefinition(name = "Watchdog", description = "The watchdog configured at the inverter to return into internal operation mode.")
	int watchdog() default 30;

	@AttributeDefinition(name = "Tolerance", description = "The tolerance value in watts to skip the modbus writing if the timer is not yet elapsed (smart-mode). Values within +/- tolerance are set to 0 (idle zone).")
	int tolerance() default 50;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device.")
	int modbusUnitId() default 71;

	@AttributeDefinition(name = "Capacity", description = "Capacity of the battery in [Wh]")
	int capacity() default 10_000;

	String webconsole_configurationFactory_nameHint() default "KOSTAL Plenticore ESS [{id}]";
}
