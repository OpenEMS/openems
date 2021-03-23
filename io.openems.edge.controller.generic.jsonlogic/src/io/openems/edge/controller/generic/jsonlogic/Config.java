package io.openems.edge.controller.generic.jsonlogic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Generic JsonLogic", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlJsonLogic0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "JsonLogic Rule?", description = "Holds the JsonObject representing a JsonLogic rule")
	String rule() default "{}";

	String webconsole_configurationFactory_nameHint() default "Controller Generic JsonLogic [{id}]";

}