package io.openems.edge.kostal.piko.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "KOSTAL PIKO ESS", //
		description = "The energy storage system implementation of a KOSTAL PIKO.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String core_id() default "kostalPiko0";

	String webconsole_configurationFactory_nameHint() default "KOSTAL PIKO ESS [{id}]";
}