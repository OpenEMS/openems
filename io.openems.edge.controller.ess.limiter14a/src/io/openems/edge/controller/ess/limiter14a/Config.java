package io.openems.edge.controller.ess.limiter14a;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Limiter §14a", //
		description = "Established by law (for Germany), this controller lowers active power to -4200W in response to grid operator limitations, aiming to alleviate load on transformers.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEssLimiter14a0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess.")
	String ess_id() default "ess0";

	@AttributeDefinition(name = "Input Channel", description = "When receiveing a signal, this channel triggers the execution of the limitation.")
	String inputChannelAddress();

	String webconsole_configurationFactory_nameHint() default "Controller Ess Limiter §14a [{id}]";

}