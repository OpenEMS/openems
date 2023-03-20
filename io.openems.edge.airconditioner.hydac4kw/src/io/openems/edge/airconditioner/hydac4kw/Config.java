package io.openems.edge.airconditioner.hydac4kw;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Air conditioner Hydac 4kW 4701290", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io.openems.edge.airconditioner.hydac4kw0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	String webconsole_configurationFactory_nameHint() default "io.openems.edge.airconditioner.hydac4kw [{id}]";

}