package io.openems.edge.io.shelly.shellyplug;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "IO Shelly Plug", //
		description = "Implements the Shelly Plug / PlugS WiFi Switch.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this Shelly Plug connected to?")
	SinglePhase phase() default SinglePhase.L1;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	String webconsole_configurationFactory_nameHint() default "IO Shelly Plug [{id}]";
}