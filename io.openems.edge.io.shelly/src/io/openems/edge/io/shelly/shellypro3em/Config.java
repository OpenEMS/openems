package io.openems.edge.io.shelly.shellypro3em;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.meter.api.MeterType;

@ObjectClassDefinition(
		name = "Shelly Pro 3EM", //
		description = "Implements the Shelly Pro 3EM Energy Meter.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "meter0"; 

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default ""; 

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true; 

	@AttributeDefinition(name = "Meter-Type", description = "Grid, Production (=default), Consumption") 
	MeterType type() default MeterType.GRID; 

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Shelly device.")
	String ip();

	String webconsole_configurationFactory_nameHint() default "Shelly Pro 3EM [{id}]"; 

}