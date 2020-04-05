package io.openems.edge.core.cycle;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.cycle.Cycle;

@ObjectClassDefinition(//
		name = "Core Cycle", //
		description = "The global OpenEMS Cycle.")
@interface Config {

	@AttributeDefinition(name = "Cycle-Time", description = "The duration of one global OpenEMS Cycle in [ms]")
	int cycleTime() default Cycle.DEFAULT_CYCLE_TIME;

	String webconsole_configurationFactory_nameHint() default "Core Cycle";

}