package io.openems.edge.scheduler.fixedorder;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.scheduler.api.Scheduler;

@ObjectClassDefinition
@interface Config {
	String id();

	boolean enabled() default true;

	int cycleTime() default Scheduler.DEFAULT_CYCLE_TIME;

	String[] controllers_ids() default {};
}