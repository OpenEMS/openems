package io.openems.edge.scheduler.allalphabetically;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "Scheduler All Alphabetically", //
		description = "This Scheduler takes an ordered list of Component IDs. All remaining Controllers are afterwards ordered alphabetically by their ID.")
@interface Config {
	String id() default "scheduler0";

	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Scheduler All Alphabetically [{id}]";
}