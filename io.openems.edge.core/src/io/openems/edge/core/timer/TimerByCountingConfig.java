package io.openems.edge.core.timer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Timer By Counting", //
		description = "This Timer is used to Count. "
				+ "Each time an OpenEMS Component calls this Timer a mapped counter is started. "
				+ "When the Counter reached its configured maximum -> the Time is up.")
@interface TimerByCountingConfig {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timerByCounting0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Timer By Counting {id}";
}
