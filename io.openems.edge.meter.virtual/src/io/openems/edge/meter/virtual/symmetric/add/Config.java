package io.openems.edge.meter.virtual.symmetric.add;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(//
		name = "Meter Virtual Symmetric Add", //
		description = "This is a virtual meter which is used to sum up the values from multiple symmetric meters")
@interface Config {

	String id() default "meter0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (=default), Consumption")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Meter IDs", description = "Ids of the meters to be summed up")
	String[] meterIds();

	String webconsole_configurationFactory_nameHint() default "Meter Virtual Symmetric Add [{id}]";

}
