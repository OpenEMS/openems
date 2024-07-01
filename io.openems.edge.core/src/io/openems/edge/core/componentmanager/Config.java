package io.openems.edge.core.componentmanager;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Component-Manager", //
		description = "The global manager for OpenEMS Components.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Component-Manager";

}