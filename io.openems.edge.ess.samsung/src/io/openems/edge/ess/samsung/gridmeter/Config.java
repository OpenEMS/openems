package io.openems.edge.ess.samsung.gridmeter;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "Samsung ESS Grid-Meter", //
		description = "Implements the Samsung ESS Grid-Meter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the ESS.")
	String ip();

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.GRID;

	String webconsole_configurationFactory_nameHint() default "Samsung ESS Grid-Meter [{id}]";
}