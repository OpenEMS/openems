package io.openems.edge.meter.discovergy;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
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

	@AttributeDefinition(name = "Discovergy MeterId", description = "Internal MeterId. This is a hex string with length 32.", required = false)
	String meterId() default "";

	@AttributeDefinition(name = "Discovergy Serial-Number", description = "Serial-Number of the meter, e.g. 12345678. See https://my.discovergy.com/readings", required = false)
	String serialNumber() default "";

	@AttributeDefinition(name = "Discovergy Full Serial-Number", description = "Full Serial-Number of the meter, e.g. 1ESY1234567890.", required = false)
	String fullSerialNumber() default "";

	String webconsole_configurationFactory_nameHint() default "Meter Discovergy [{id}]";
}