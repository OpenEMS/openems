package io.openems.edge.controller.io.alarm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller IO Alarm", //
		description = "Switches a digital output, when one or more State-Channels are set. Can be used to signal alarms.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ctrlIoAlarm0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Input Channels", description = "Addresses of the input State-Channels")
	String[] inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();

	String webconsole_configurationFactory_nameHint() default "Controller IO Alarm [{id}]";

}
