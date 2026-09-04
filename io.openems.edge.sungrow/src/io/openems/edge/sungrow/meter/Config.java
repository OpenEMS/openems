package io.openems.edge.sungrow.meter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Ess Sungrow Grid Meter", //
		description = "Implements grid meter from Sungrow Hybrid ESS.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "ESS-ID", description = "ID of the Sungrow Ess Component.")
	String ess_id() default "ess0";

	String webconsole_configurationFactory_nameHint() default "Ess Sungrow Grid Meter [{id}]";

}
