package io.openems.edge.ess.cluster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "ESS Cluster", //
		description = "Combines several energy storage systems to one.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "ESS-IDs", description = "IDs of Ess devices.")
	String[] ess_ids();

	String webconsole_configurationFactory_nameHint() default "ESS Cluster [{id}]";
}