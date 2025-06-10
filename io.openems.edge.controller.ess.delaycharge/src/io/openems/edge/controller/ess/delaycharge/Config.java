package io.openems.edge.controller.ess.delaycharge;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Ess Delay Charge", //
		description = "Delays charging to 100 % SoC till a certain hour of the day.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlDelayCharge0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Target hour", description = "Charging to 100 % SoC is delayed till this hour of the day, e.g. 15 for 3 pm. Local timezone of this device is applied - likely UTC.")
	int targetHour() default 15;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Delay Charge [{id}]";

}