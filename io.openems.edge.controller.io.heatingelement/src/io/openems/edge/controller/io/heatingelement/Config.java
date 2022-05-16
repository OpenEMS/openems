package io.openems.edge.controller.io.heatingelement;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.io.heatingelement.enums.Level;
import io.openems.edge.controller.io.heatingelement.enums.Mode;
import io.openems.edge.controller.io.heatingelement.enums.WorkMode;

@ObjectClassDefinition(//
		name = "Controller IO Heating Element", //
		description = "Controls a three-phase heating element via Relays, according to grid active power")
@interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoHeatingElement0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Mode", description = "Set the type of mode.")
	Mode mode() default Mode.AUTOMATIC;

	@AttributeDefinition(name = "Output Channel Phase L1", description = "Channel address of the Digital Output for Phase L1")
	String outputChannelPhaseL1() default "io0/Relay1";

	@AttributeDefinition(name = "Output Channel Phase L2", description = "Channel address of the Digital Output for Phase L2")
	String outputChannelPhaseL2() default "io0/Relay2";

	@AttributeDefinition(name = "Output Channel Phase L3", description = "Channel address of the Digital Output for Phase L3")
	String outputChannelPhaseL3() default "io0/Relay3";

	@AttributeDefinition(name = "Default Level", description = "This is the default Level in manual mode and for force-heating in automatic mode")
	Level defaultLevel() default Level.LEVEL_1;

	@AttributeDefinition(name = "End Time", description = "End time for minimum run time")
	String endTime() default "17:00";

	@AttributeDefinition(name = "Work-Mode Time or None", description = "Sets the Work-Mode to Time (= run at least Minimum Time) or None (only run on excess power)")
	WorkMode workMode() default WorkMode.TIME;

	@AttributeDefinition(name = "Minimum Time [h]", description = "For Work-Mode 'Time': Minimum Time in hours for activating 'Levels'")
	int minTime() default 1;

	@AttributeDefinition(name = "Power per Phase", description = "Power of one single phase of the heating element in [W]")
	int powerPerPhase() default 2000;

	@AttributeDefinition(name = "Minimum switching time between two states", description = "Minimum time (Seconds) is applied to avoid continuous switching on threshold")
	int minimumSwitchingTime() default 60;

	String webconsole_configurationFactory_nameHint() default "Controller IO Heating Element [{id}]";
}