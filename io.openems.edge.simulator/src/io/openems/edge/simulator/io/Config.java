package io.openems.edge.simulator.io;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator IO Digital", //
		description = "Simulates digital input/output channels with name 'InputOutputX', starting with index 0.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "io0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Number of input/output channels", description = "This many channels 'InputOutputX' are created.")
	int numberOfOutputs() default 1;

	String webconsole_configurationFactory_nameHint() default "Simulator IO Digital [{id}]";

}
