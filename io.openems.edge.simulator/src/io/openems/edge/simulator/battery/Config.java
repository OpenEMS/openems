package io.openems.edge.simulator.battery;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "BMS Simulated", //
		description = "Implements a simulated battery management system")
@interface Config {
	String service_pid();

	String id() default "bms0";

	boolean enabled() default true;
	
	int capacity_kWh() default 10;

	String webconsole_configurationFactory_nameHint() default "BMS Simulated [{id}]";
}