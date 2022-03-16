package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timer By Cycles", //
		description = "This Timer counts Cycles. " //
				+ "For each Component added to this Timer, a Cycle Counter will start. " //
				+ "When the Maximum Cycles are reached. The Components know, that the time is up.")
@interface TimerByCyclesConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timerByCycles0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Timer By Cycles {id}";
}
