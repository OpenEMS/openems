package io.openems.edge.evcc.api.gridtariff;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(//
		name = "Time-Of-Use Grid Tariff evcc-API", //
		description = "implements Time-Of-Use Grid Tariff by evcc-API")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "timeOfUseTariff0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "URL", description = "URL for grid tariff api from evcc; defaults to localhost API-URL")
	String apiUrl() default "http://localhost:7070/api/tariff/grid";

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "TimeOfUseGridTariff evcc [{id}]";
}
