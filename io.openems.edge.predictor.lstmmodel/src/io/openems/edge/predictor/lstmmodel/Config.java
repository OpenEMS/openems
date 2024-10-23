package io.openems.edge.predictor.lstmmodel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(//
		name = "Predictor Lstm-Model", //
		description = "Implements Long Short-Term Memory (LSTM) model, which is a type of recurrent neural network (RNN) designed to capture long-range dependencies in sequential data, such as time series. "
				+ "This makes LSTMs particularly effective for time series prediction, "
				+ "as they can learn patterns and trends over time, handling long-term dependencies while filtering out irrelevant information.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Channel-Address", description = "Channel-Address this Predictor is used for, e.g. '_sum/UnmanagedConsumptionActivePower'")
	String channelAddress();

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "Predictor Lstm-Model [{id}]";
}