package io.openems.edge.meter.tibber.pulse;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "Meter Tibber Pulse", //
		description = "Implements the Tibber Pulse as Meter")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP", description = "IP of the Tibber bridge.")
	String ip() default "";
	
	@AttributeDefinition(name = "Password", description = "Password of the Tibber bridge",  type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.GRID;

	String webconsole_configurationFactory_nameHint() default "Meter Tibber Pulse [{id}]";

}