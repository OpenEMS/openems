package io.openems.edge.evcc.api.solartariff;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(//
		name = "Predictor Solar Production (evcc-API)", //
		description = "Provides solar production predictions using the evcc-API.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "URL", description = "URL for solar tariff api from evcc; defaults to localhost API-URL")
	String url() default "http://localhost:7070/api/tariff/solar";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;

	String webconsole_configurationFactory_nameHint() default "Predictor SolarTariff evcc [{id}]";

}
