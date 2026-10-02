package io.openems.edge.fluidcooling.hydac;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "Cooling Unit Hydac", //
		description = "Implements the cooling unit Hydac")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component.")
	String id() default "fluidCooling0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID.")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.AUTO;

	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Temperature Setpoint", description = "The setpoint for the cooling temperature. Scaling 0,01 (2500 = 25 Degree)")
	int temperatureSetpoint() default 2500;

	String webconsole_configurationFactory_nameHint() default "Cooling Unit Hydac[{id}]";

}
