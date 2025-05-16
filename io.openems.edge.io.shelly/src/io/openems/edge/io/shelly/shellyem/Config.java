package io.openems.edge.io.shelly.shellyem;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "IO Shelly EM (Gen.1)", //
		description = "Implements the Shelly EM (Gen.1)")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;

	@AttributeDefinition(name = "Summarize energy meters", description = "Whether to sum the values from first and second energy meter.")
	boolean sumEmeter1AndEmeter2() default false;

	@AttributeDefinition(name = "Measuring channel", description = "Which channel should be measured? Only relevant if sum is set to false")
	int channel() default 0;

	@AttributeDefinition(name = "Phase", description = "Which phase is the Shelly EM connected?")
	SinglePhase phase() default SinglePhase.L1;

	String webconsole_configurationFactory_nameHint() default "IO Shelly EM [{id}]";
}