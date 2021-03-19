package io.openems.edge.controller.symmetric.dynamiccharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Dynamic Charge Symmetric", //
		description = "Awattar Austria")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDynamicCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start-Hour", description = "Fallback start hour if no pv.")
	int maxStartHour() default 8;

	@AttributeDefinition(name = "End-Hour", description = "fallback end hour if no pv.")
	int maxEndHour() default 17;

	String webconsole_configurationFactory_nameHint() default "Controller Dynamic Charge Symmetric [{id}]";

}