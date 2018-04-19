package io.openems.edge.scheduler.fixedorder;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition( //
		name = "Fixed Order Scheduler", //
		description = "This Scheduler takes a list of Component IDs and returns the Controllers statically sorted by this order.")
@interface Config {
	String service_pid();

	String id();

	boolean enabled();

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Fixed Order Scheduler [{id}]";
}