package io.openems.edge.ess.samsung.ess;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.SinglePhase;

@ObjectClassDefinition(//
		name = "ESS Samsung", //
		description = "Implements the Sasmung ESS Combined System.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "ess0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Phase", description = "Which Phase is this ESS connected to?")
	SinglePhase phase() default SinglePhase.L1;
	
	@AttributeDefinition(name = "Capacity", description = "The Capacity of the ESS in Wh")
	int capacity() default 47000;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the ESS.")
	String ip();
    
	String webconsole_configurationFactory_nameHint() default "Samsung ESS Device [{id}]";
}