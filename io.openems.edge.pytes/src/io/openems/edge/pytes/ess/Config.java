package io.openems.edge.pytes.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Pytes Hybrid Inverter", //
		description = "Pytes Hybrid Inverter")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;


	@AttributeDefinition(name = "Max. Apparent Power", description = "Inverter´s apparent power limit")
	int maxApparentPower() default 10000;	
	
	String webconsole_configurationFactory_nameHint() default "io.openems.edge.pytes [{id}]";

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "ReadOnly Mode", description = "read only mode")
	boolean readOnlyMode() default false;	
	
	@AttributeDefinition(name = "Modbus-ID", description = "ID of Modbus bridge.")
	String modbus_id() default "modbus0";

	@AttributeDefinition(name = "Modbus Unit-ID", description = "The Unit-ID of the Modbus device. ")
	int modbusUnitId() default 1;

}