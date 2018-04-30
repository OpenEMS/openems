package io.openems.edge.scheduler.allalphabetically;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "All Alphabetically Scheduler", //
		description = "This Scheduler returns all existing Controllers ordered by their ID.")
@interface Config {
	String service_pid();

	String id() default "scheduler0";

	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	String webconsole_configurationFactory_nameHint() default "All Alphabetically Scheduler [{id}]";
}