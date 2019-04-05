package io.openems.edge.scheduler.dailyscheduler;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "Scheduler Daily Scheduler", description = "")

@interface Config {

	String id() default "dailyScheduler0";

	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers.")
	String controllers_ids_json() default "";

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Daily Scheduler [{id}]";

}
