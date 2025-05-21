package io.openems.edge.predictor.persistencemodel;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(//
		name = "Predictor Persistence-Model", //
		description = "")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Channel-Addresses", description = "List of Channel-Addresses this Predictor is used for, e.g. '*/ActivePower', '*/ActualPower'")
	String[] channelAddresses() default { //
			"_sum/ProductionActivePower", //
			"_sum/UnmanagedConsumptionActivePower", //
			"_sum/ConsumptionActivePower" };

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "Predictor Persistence-Model [{id}]";

}