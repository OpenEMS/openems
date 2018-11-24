package io.openems.edge.simulator.io;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition( //
		name = "Simulator IO Digital Output", //
		description = "Simulates digital outputs with name 'DigitalOutputX', starting with index 0.")
@interface Config {
	String service_pid();

	String id()

	default "io0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Number of outputs", description = "This many output channels 'DigitalOutputX' are created.")
	int numberOfOutputs() default 1;

	String webconsole_configurationFactory_nameHint() default "Simulator IO Digital Output [{id}]";

}
