package io.openems.edge.predictor.weather.forecast;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import io.openems.edge.predictor.api.prediction.LogVerbosity;

@ObjectClassDefinition(//
		name = "PV Predictor- Weather Forecast-Model from Openmeteo", //
		description = "PV Production Power Prediction using Openmeteo weather forecast api")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Channel-Addresses", description = "List of Channel-Addresses this Predictor is used for, e.g. '*/ActivePower', '*/ActualPower'")
	String[] channelAddresses() default { //
			"_sum/ProductionActivePower"};

	@AttributeDefinition(name = "Log-Verbosity", description = "The log verbosity.")
	LogVerbosity logVerbosity() default LogVerbosity.NONE;
	
	@AttributeDefinition(name = "latitude", description = "Geographic latitude coordinate. Ex. 52.52")
	String  latitude() default "25.230001";
	
	@AttributeDefinition(name = "latitude", description = "Geographic longitude coordinate. Ex. 13.41")
	String longitude() default "15.455001";
	
	@AttributeDefinition(name = "Multiplication Factor", description = "multiplication factor to estimate the PV production power from short wave solar radiation, "
			+ "m² is the size of each PV_Panel * number of PV-Panels * Efficiency Factor (can be estimated from historical values , KW)")
	double factor() default 1.00 * 10.0 * 0.20;

	String webconsole_configurationFactory_nameHint() default "Predictor Weather Forecast-Model [{id}]";

}
