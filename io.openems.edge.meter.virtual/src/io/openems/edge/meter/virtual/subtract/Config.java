
package io.openems.edge.meter.virtual.subtract;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;

@ObjectClassDefinition(//
		name = "Meter Virtual Subtract", //
		description = """
				This is a virtual meter built from subtracting other meters or energy storage systems. \
				The logic calculates `Minuend - Subtrahend1 - Subtrahend2 - ...`. \
				Example use-case: create a virtual Grid-Meter from Production-Meter, Consumption-Meter and \
				Energy Storage System by configuring the Consumption-Meter as Minuend and Production-Meter and ESS as Subtrahends.""")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (=default), Consumption")
	MeterType type() default MeterType.PRODUCTION;

	@AttributeDefinition(name = "Minuend-ID", description = "Component-ID of the minuend; if empty '0' power is assumed")
	String minuend_id();

	@AttributeDefinition(name = "Subtrahends-IDs", description = "Component-IDs of the subtrahends")
	String[] subtrahends_ids();

	@AttributeDefinition(name = "Add to Sum?", description = "Should the data of this meter be added to the Sum?")
	boolean addToSum() default false;

	String webconsole_configurationFactory_nameHint() default "Meter Virtual Subtract [{id}]";

}
