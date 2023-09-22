package io.openems.edge.battery.fenecon.f2b.dummy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "Battery FENECON F2B Dummy", //
		description = "Provides a dummy battery to test the applications or controllers which require a f2b battery component")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.AUTO;

}