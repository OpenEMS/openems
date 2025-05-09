package io.openems.edge.io.shelly.shellyem;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.common.types.MeterType;
import io.openems.edge.meter.api.SinglePhase;  

@ObjectClassDefinition(//
		name = "IO Shelly EM 1. Gen", //
		description = "Implements the Shelly EM (1. Gen)")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	@AttributeDefinition(name = "Meter-Type", description = "What is measured by this Meter?")
	MeterType type() default MeterType.CONSUMPTION_METERED;
	
	@AttributeDefinition(name = "Sum emeter1 and emeter2", description = "Whether to sum the values from emeter1 and emeter2")  
	boolean sumEmeter1AndEmeter2() default false;
	
	@AttributeDefinition(name = "Measuring channel", description = "Which channel should be measured? Only relevant if Sum is set to false")  
	int channel() default 0;

    @AttributeDefinition(name = "Phase", description = "Which phase is the shelly em connected?")  
    SinglePhase phase() default SinglePhase.L1;
	
	String webconsole_configurationFactory_nameHint() default "IO Shelly EM [{id}]";
}