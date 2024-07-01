package io.openems.edge.evcs.core;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVCS Power", //
		description = "This component defines the increase rate for the ramp filter for every EVCS Component.")
@interface Config {

	@AttributeDefinition(name = "Enable Slow Power Increase Filter", description = "Enables the Slow Power Increase Filter with the settings for the increasing rate below")
	boolean enableSlowIncrease() default false;

	@AttributeDefinition(name = "Rate of increase", description = "The rate of increase between 0 and 1.")
	float increaseRate() default 0.05f;

	String webconsole_configurationFactory_nameHint() default "EVCS Slow Power Increase Filter";

}
