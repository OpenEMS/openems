package io.openems.edge.simulator.io;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator IO Digital", //
		description = "Simulates digital input/output channels with name 'InputOutputX', starting with index 0.")
@interface Config {
	
	String service_pid();

	String id()

	default "io0";

	boolean enabled() default true;

	@AttributeDefinition(name = "Number of input/output channels", description = "This many channels 'InputOutputX' are created.")
	int numberOfOutputs() default 1;

	String webconsole_configurationFactory_nameHint() default "Simulator IO Digital [{id}]";

}
