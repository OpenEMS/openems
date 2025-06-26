package io.openems.edge.simulator.app;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulator App", //
		description = """
				The Simulator-App is a very specific component that needs to be handled with care. \
				It provides a full simulation environment to run an OpenEMS Edge instance in simulated \
				realtime environment. \
				CAUTION: Be aware that the SimulatorApp Component takes control over the complete \
				OpenEMS Edge Application, i.e. if you enable it, it is going to *delete all existing \
				Component configurations*!""")
@interface Config {

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default false;

	String webconsole_configurationFactory_nameHint() default "Simulator App [{id}]";

}
