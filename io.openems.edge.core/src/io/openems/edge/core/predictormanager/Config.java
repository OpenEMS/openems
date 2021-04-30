package io.openems.edge.core.predictormanager;

import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Predictor-Manager", //
		description = "The global manager for Predictors.")
@interface Config {

	String webconsole_configurationFactory_nameHint() default "Core Predictor-Manager";

}