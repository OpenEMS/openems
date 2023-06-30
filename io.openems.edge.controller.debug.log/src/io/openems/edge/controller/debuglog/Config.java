package io.openems.edge.controller.debuglog;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Debug Log", //
		description = "This controller prints information about all available components on the console")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDebugLog0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Show Alias", description = "Print the Alias for each Component")
	boolean showAlias() default false;

	@AttributeDefinition(name = "Condensed output", description = "Print all logs in one joint line")
	boolean condensedOutput() default true;

	@AttributeDefinition(name = "Additional Channels", description = "Channel-Addresses of additional Channels that should be logged")
	String[] additionalChannels() default {};

	@AttributeDefinition(name = "Ignore Components", description = "Component-IDs of Components that should not be logged. Accepts '*' wildcard.")
	String[] ignoreComponents() default {};

	String webconsole_configurationFactory_nameHint() default "Controller Debug Log [{id}]";
}