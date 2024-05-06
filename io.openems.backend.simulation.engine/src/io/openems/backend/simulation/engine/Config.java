package io.openems.backend.simulation.engine;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Simulation Engine", //
		description = "Simulation Engine" //
)
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Simulation Engine";

}