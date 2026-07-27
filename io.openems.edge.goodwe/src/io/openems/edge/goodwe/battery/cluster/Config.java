package io.openems.edge.goodwe.battery.cluster;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "GoodWe Battery Cluster FENECON Home", //
		description = "Combines several FENECON Home batteries devices to one, connected on one GoodWe BatteryInverter.") //

public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start/Stop behaviour?", description = "Should this component be forced to start or stop?")
	StartStopConfig startStop() default StartStopConfig.AUTO;

	@AttributeDefinition(name = "Ordered Battery IDs", description = "IDs of battery devices ordered from battery 1, 2, ...")
	String[] battery_ids();

	String webconsole_configurationFactory_nameHint() default "GoodWe Battery Cluster FENECON Home [{id}]";

}