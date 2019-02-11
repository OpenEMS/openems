package io.openems.edge.simulator.modbus;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator Modbus Bridge", //
		description = "This simulates a Modbus Bridge")
@interface Config {
	String id() default "modbus0";

	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Simulator Modbus Bridge [{id}]";
}