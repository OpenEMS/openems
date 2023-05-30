package io.openems.edge.battery.dummy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "Dummy Battery", //
		description = "Provides a dummy battery to test the applications or controllers which required a battery component")
public @interface Config {
	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.AUTO;

	@AttributeDefinition(name = "Battery Start Time?", description = "Battery will be running or stopped after this duration in seconds.")
	int batteryStartStopTime() default 10;
}