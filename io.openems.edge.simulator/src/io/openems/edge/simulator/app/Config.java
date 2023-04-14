package io.openems.edge.simulator.app;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator App", //
		description = """
			The Simulator-App is a very specific component that needs to be handled with care.\s\
			It provides a full simulation environment to run an OpenEMS Edge instance in simulated\s\
			realtime environment.\s\
			CAUTION: Be aware that the SimulatorApp Component takes control over the complete\s\
			OpenEMS Edge Application, i.e. if you enable it, it is going to *delete all existing\s\
			Component configurations*!""")
@interface Config {

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default false;

	String webconsole_configurationFactory_nameHint() default "Simulator App [{id}]";

}
