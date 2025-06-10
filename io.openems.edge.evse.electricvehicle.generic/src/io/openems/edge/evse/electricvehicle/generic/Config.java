package io.openems.edge.evse.electricvehicle.generic;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "EVSE Electric-Vehicle Generic", //
		description = "A generic Electric-Vehicle")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "evseElectricVehicle0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Maximum Current in Single-Phase charging [mA]", description = "")
	int maxCurrentSinglePhase() default 32000;

	@AttributeDefinition(name = "Maximum Current in Three-Phase charging [mA]", description = "0 if Three-Phase charging is not available")
	int maxCurrentThreePhase() default 0;

	@AttributeDefinition(name = "Does this EV support interrupting a charging session?", description = "Some elder EVs do not support interrupting")
	boolean canInterrupt() default true;

	String webconsole_configurationFactory_nameHint() default "EVSE Electric-Vehicle Generic [{id}]";
}