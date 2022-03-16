package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timer By Time", //
		description = "This Timer is like a Stopwatch, on initialization "
				+ "it saves the Calling Time, whenever the corresponing OpenEMS component asks if the time is up, it checks "
				+ "the initialize Time with the waitTime corresponding to the Component."
				+ "The Time is in Seconds and the waitTime is configured individually by the OpenEMS Components themself.")
@interface TimerByTimeConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timerByTime0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Timer By Time {id}";
}
