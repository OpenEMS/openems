package io.openems.edge.core.predictormanager;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core ComponentManager", //
		description = "The global OpenEMS manager for Component.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core ComponentManager";

}