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

	@AttributeDefinition(name = "Minimum Power in Single-Phase charging [W]", description = "1380 W for 6 A, 2300 W for 10 A")
	int minPowerSinglePhase() default 1380;

	@AttributeDefinition(name = "Maximum Power in Single-Phase charging [W]", description = "3680 W for 16 A, 7360 W for 32 A")
	int maxPowerSinglePhase() default 7360;

	@AttributeDefinition(name = "Minimum Power in Three-Phase charging [W]", description = "4140 W for 6 A, 6900 W for 10 A")
	int minPowerThreePhase() default 4140;

	@AttributeDefinition(name = "Maximum Power in Three-Phase charging [W]", description = "11040 W for 16 A, 22080 W for 32 A, 0 if Three-Phase charging is not available")
	int maxPowerThreePhase() default 11040;

	@AttributeDefinition(name = "Does this EV support interrupting a charging session?", description = "Some elder EVs do not support interrupting")
	boolean canInterrupt() default true;

	String webconsole_configurationFactory_nameHint() default "EVSE Electric-Vehicle Generic [{id}]";
}