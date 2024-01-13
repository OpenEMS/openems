package io.openems.edge.batteryinverter.cluster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "Battery Inverter Cluster", //
		description = "Combines several battery inverters into one. Should be used only for parallel inverters connected to a single battery")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "batteryInverter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this Component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.START;

	@AttributeDefinition(name = "BatteryInverter-IDs", description = "The IDs of the battery inverter")
	String[] batteryInverterIds();

	String webconsole_configurationFactory_nameHint() default "Battery Inverter Cluster [{id}]";

}