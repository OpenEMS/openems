package io.openems.edge.example.gruenstrom;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(name = "GruenStrom Reader", //
		description = "Implements a reader that checks how green the elecricity of a given area is.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "gstr0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Postal Code", description = "The Postal Code of to be read.", required = true)
	String plz() default "94469";

	String webconsole_configurationFactory_nameHint() default "GruenStrom Reader [{id}]";
}