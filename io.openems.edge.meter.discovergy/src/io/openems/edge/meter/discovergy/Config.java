package io.openems.edge.meter.discovergy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition( //
		name = "Meter Discovergy", //
		description = "Implements the Discovergy smart meter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Authentication E-Mail", description = "E-Mail for your my.discovergy.com access.")
	String email() default "";

	@AttributeDefinition(name = "Authentication Password", description = "Password for your my.discovergy.com access.", type = AttributeType.PASSWORD)
	String password() default "";

	@AttributeDefinition(name = "Discovergy MeterId", description = "Internal MeterId. If not provided, the first meter is taken", required = false)
	String meterId() default "";

	String webconsole_configurationFactory_nameHint() default "Meter Discovergy [{id}]";
}