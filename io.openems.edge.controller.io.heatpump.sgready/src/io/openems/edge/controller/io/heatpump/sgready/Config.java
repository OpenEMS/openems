package io.openems.edge.controller.io.heatpump.sgready;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller IO SG-Ready Heat Pump", //
		description = "Controls a SG-Ready heat pump via two relay outputs, depending on surplus power and battery state-of-charge.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoHeatPump0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;

	@AttributeDefinition(name = "Output Channel 1", description = "Channel address of the Digital Output for input 1.")
	String outputChannel1() default "io0/Relay2";

	@AttributeDefinition(name = "Output Channel 2", description = "Channel address of the Digital Output for input 2.")
	String outputChannel2() default "io0/Relay3";

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Manual State", description = "Set the State used in the manual mode.")
	Status manualState() default Status.REGULAR;

	@AttributeDefinition(name = "Recommendation control enabled?", description = "Is the recommendation control enabled? (In the automatic mode)")
	boolean automaticRecommendationCtrlEnabled() default true;

	@AttributeDefinition(name = "Recommendation surplus power", description = "Above this surplus power, the recommendation state will be set. WARNING: This value should be not lower than the real used power in the recommendation mode!")
	int automaticRecommendationSurplusPower() default 3000;

	@AttributeDefinition(name = "Force on control enabled?", description = "Is the force on control enabled? (In the automatic mode)")
	boolean automaticForceOnCtrlEnabled() default true;

	@AttributeDefinition(name = "Force on surplus power", description = "Above this surplus power and the soc is high enough, the force on state will be set. WARNING: This value should be not lower than the real used power in the force on mode! (heat pump + extra heaters...)")
	int automaticForceOnSurplusPower() default 5000;

	@AttributeDefinition(name = "Force on SoC", description = "Above this state of charge power and enough surplus power is present, the force on state will be set. WARNING: This value should be not lower than the real used power in the force on mode! (heat pump + extra heaters...). If soc control is not needed, it can be set to 0.")
	int automaticForceOnSoc() default 10;

	@AttributeDefinition(name = "Lock control enabled?", description = "Is the lock control enabled? (In the automatic mode)")
	boolean automaticLockCtrlEnabled() default false;

	@AttributeDefinition(name = "Lock grid buy power", description = "Above this grid buy power and the soc is low enough, the lock state will be set.")
	int automaticLockGridBuyPower() default 5000;

	@AttributeDefinition(name = "Lock SoC", description = "Below this state of charge and above the grid buy power, the lock state will be set. If soc control is not needed, it can be set to 100.")
	int automaticLockSoc() default 20;

	@AttributeDefinition(name = "Minimum switching time between two states", description = "Minimum time (Seconds) is applied to avoid continuous switching on threshold")
	int minimumSwitchingTime() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller IO SG-Ready Heat Pump [{id}]";

}