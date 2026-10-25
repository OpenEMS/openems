package io.openems.edge.sonnen.pv;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Sonnen PV-Meter", //
		description = "PV production, measured by the sonnenBatterie.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "pv0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Core-ID", description = "Component-ID of the Sonnen Core component")
	String core_id() default "sonnenCore0";

	String webconsole_configurationFactory_nameHint() default "Sonnen PV-Meter [{id}]";
}
