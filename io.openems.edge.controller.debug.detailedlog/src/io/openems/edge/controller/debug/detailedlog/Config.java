package io.openems.edge.controller.debug.detailedlog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Debug Detailed Log", //
		description = "This controller prints detailed information about the defined components on the console")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDetailedLog0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Component-IDs", description = "IDs of OpenemsComponents.")
	String[] component_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Controller Debug Detailed Log [{id}]";
}