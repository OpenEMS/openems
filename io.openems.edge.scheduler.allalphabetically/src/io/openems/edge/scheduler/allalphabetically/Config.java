package io.openems.edge.scheduler.allalphabetically;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Scheduler All Alphabetically", //
		description = "This Scheduler takes an ordered list of Component IDs. All remaining Controllers are afterwards ordered alphabetically by their ID.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "scheduler0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Controller-IDs", description = "IDs of Controllers. Controller execution is going to be sorted in the order of the IDs.")
	String[] controllers_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Scheduler All Alphabetically [{id}]";
}