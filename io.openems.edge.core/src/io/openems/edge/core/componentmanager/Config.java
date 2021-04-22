package io.openems.edge.core.componentmanager;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core PredictorManager", //
		description = "The global OpenEMS manager for Predictors.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core PredictorManager";

}