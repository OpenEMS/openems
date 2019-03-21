package io.openems.edge.controller.io.alarm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller IO Alarm",
		description = "The controller to read state channels and signal alarms"
		)

@interface Config {
	String id() default "ctrlIOAlarm0";
	
	boolean enabled() default true;
	
	
	@AttributeDefinition(name = "Input Channel", description = "Address of the input channel")
	String[] inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "Channel address of the Digital Output that should be switched")
	String outputChannelAddress();
	
	String webconsole_configurationFactory_nameHint() default "Controller IO Alarm [{id}]";
	
}
