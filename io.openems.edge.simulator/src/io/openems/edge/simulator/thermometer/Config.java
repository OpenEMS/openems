package io.openems.edge.simulator.thermometer;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator Thermometer", //
		description = "Simulates digital input/output channels with name 'InputOutputX', starting with index 0.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "temp0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Temperature", description = "The simulated temperature in 0.1 degree celsius")
	int temperature();

	String webconsole_configurationFactory_nameHint() default "Simulator Thermometer [{id}]";

}
