package io.openems.edge.battery.fenecon.f2b.cluster.parallel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.common.startstop.StartStopConfig;

@ObjectClassDefinition(//
		name = "Battery FENECON F2B Parallel Cluster", //
		description = "Combines several parallel connected battery devices or battery serial clusters to one.") //

public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "battery0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Start/stop behaviour?", description = "Should this component be forced to start or stop ?")
	StartStopConfig startStop() default StartStopConfig.AUTO;

	@AttributeDefinition(name = "Battery IDs", description = "IDs of battery devices.")
	String[] battery_ids();

	String webconsole_configurationFactory_nameHint() default "Battery FENECON F2B Parallel Cluster [{id}]";
}