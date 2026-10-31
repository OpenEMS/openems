package io.openems.edge.controller.ess.stepsoc;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.controller.ess.stepsoc.enums.Direction;

@ObjectClassDefinition(//
		name = "Controller Ess Step SoC", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlEssStepSoc0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Ess-ID", description = "ID of Ess device.")
	String ess_id();

	@AttributeDefinition(name = "Direction", description = "The direction of operation (Charge, Discharge). Note: if system is not at empty/full SoC, the controller will discharge/charge the system first to reach the correct starting point")
	Direction direction() default Direction.CHARGE;

	@AttributeDefinition(name = "Power [W]", description = "Absolute power value in Watt to be used for charging/discharging. See 'Direction' for the operation mode.")
	int power();

	@AttributeDefinition(name = "SoC Step [%]", description = "The step size in %. E.g. if 10% is chosen, the controller will charge/discharge from 0% to 10% and then pause (time given in Standby time)")
	int socStep() default 10;

	@AttributeDefinition(name = "Standby time [min]", description = "The idle time in minutes after each step")
	int standbyTime() default 10;

	String webconsole_configurationFactory_nameHint() default "Controller Ess Step SoC [{id}]";

}