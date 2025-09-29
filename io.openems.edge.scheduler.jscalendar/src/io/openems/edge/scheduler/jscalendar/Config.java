package io.openems.edge.scheduler.jscalendar;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Scheduler JSCalendar", //
		description = "This Scheduler allows enabling specific Controllers on defined dates and times using the JSCalendar format.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "scheduler0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Always Run Before", description = "IDs of Controllers that should be executed _before_ other Controllers in the order of the IDs.")
	String[] alwaysRunBeforeController_ids() default {};

	@AttributeDefinition(name = "Calendar of active controllers", description = "Takes a JSON-Array in JSCalendar format")
	String jsCalendar() default "[]";

	@AttributeDefinition(name = "Always Run After", description = "IDs of Controllers that should be executed _after_ other Controllers in the order of the IDs.")
	String[] alwaysRunAfterController_ids() default { "ctrlDebugLog0" };

	String webconsole_configurationFactory_nameHint() default "Scheduler JSCalendar [{id}]";
}