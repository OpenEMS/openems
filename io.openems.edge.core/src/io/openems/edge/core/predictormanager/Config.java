package io.openems.edge.core.predictormanager;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Core Predictor-Manager", //
		description = "The global manager for Predictors.")
@interface Config {

	@AttributeDefinition(name = "Predictor-IDs", description = "Defines the ranking of Predictor Services. Execution will follow the order of the specified IDs.")
	String[] predictor_ids() default {};

	String webconsole_configurationFactory_nameHint() default "Core Predictor-Manager";
}