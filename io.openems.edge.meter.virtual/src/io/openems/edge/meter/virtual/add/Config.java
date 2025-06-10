package io.openems.edge.meter.virtual.add;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "Meter Virtual Add", //
		description = "This is a virtual meter which is used to sum up the values from multiple meters")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid (=default), Consumption")
	MeterType type() default MeterType.GRID;

	@AttributeDefinition(name = "Meter IDs", description = "Ids of the meters to be summed up")
	String[] meterIds();

	@AttributeDefinition(name = "Add to Sum?", description = "Should the data of this meter be added to the Sum?")
	boolean addToSum() default false;

	String webconsole_configurationFactory_nameHint() default "Meter Virtual Add [{id}]";
}