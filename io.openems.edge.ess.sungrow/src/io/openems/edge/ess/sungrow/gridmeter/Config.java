package io.openems.edge.ess.sungrow.gridmeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Ess Sungrow Virtual Grid Meter", //
		description = "Implements virtual meter from Sungrow Hybrid ESS.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Core ID", description = "ID of the Sungrow Ess Component.")
	String core_id() default "ess0";

	String webconsole_configurationFactory_nameHint() default "Ess Sungrow Virtual Grid Meter [{id}]";

}
