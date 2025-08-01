package io.openems.edge.predictor.production.linearmodel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.predictor.api.prediction.SourceChannel;

@ObjectClassDefinition(//
		name = "Predictor Production Linear Model", //
		description = "Predicts production based on weather forecast data.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Source Channel", description = "Which Channel should be used as source?")
	SourceChannel sourceChannel() default SourceChannel.UNMANAGED_PRODUCTION_ACTIVE_POWER;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "Predictor Production Linear Model [{id}]";
}