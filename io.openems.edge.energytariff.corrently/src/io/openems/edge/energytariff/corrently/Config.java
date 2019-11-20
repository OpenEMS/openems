package io.openems.edge.energytariff.corrently;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Energy Tariff Corrently", //
		description = "Implements the Corrently energy tariff.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "corrently0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "ZIP code", description = "The ZIP code for the Green Power Index, e.g. 94469")
	String zipCode();

	String webconsole_configurationFactory_nameHint() default "Energy Tariff Corrently [{id}]";
}