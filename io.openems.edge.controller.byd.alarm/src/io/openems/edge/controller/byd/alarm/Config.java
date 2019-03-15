package io.openems.edge.controller.byd.alarm;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Controller Byd Alarm",
		description = "The controller to read state channels and signal alarms"
		)

@interface Config {
	String id() default "ctrlBydAlarm0";
	
	boolean enabled() default true;
	
	
	@AttributeDefinition(name = "Input Channel", description = "")
	String[] inputChannelAddress();

	@AttributeDefinition(name = "Output Channel", description = "")
	String outputChannelAddress();
	
	String webconsole_configurationFactory_nameHint() default "Controller Byd Alarm [{id}]";
	
}
