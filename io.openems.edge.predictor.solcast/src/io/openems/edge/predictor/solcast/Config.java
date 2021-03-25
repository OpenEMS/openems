package io.openems.edge.predictor.solcast;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Predictor Solcast-Model", //
		description = "Predicts the Production using the Solcast Forecast.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictorSolcast0";
	
	@AttributeDefinition(name = "Latitude", description = "Latitude for Forecast Position")
	String lat() default "52.516273";
	
	@AttributeDefinition(name = "Longitude", description = "Longitude for Forecast Position")
	String lon() default "13.381179";
	
	@AttributeDefinition(name = "API Key", description = "API-Key for Solcast")
	String key() default "";
	
	@AttributeDefinition(name = "Resource-ID", description = "Resource-ID for Solcast")
	String resource_id() default "";
	
	@AttributeDefinition(name = "API-requests are limited?", description = "API-Requests are limited and time limit is enabled")
	boolean limitedAPI() default true;
	
	@AttributeDefinition(name = "Starttime", description = "Allowed Starttime for API-Requests, will be ignored if disabled")
	String starttime() default "06:00";
	
	@AttributeDefinition(name = "Endtime", description = "Allowed Endtime for API-Requests, will be ignored if disabled")
	String endtime() default "16:00";
	
	@AttributeDefinition(name = "Channel-Addresses", description = "List of Channel-Addresses this Predictor is used for, e.g. '*/Predict', '*/Predict10'")
	String[] channelAddresses() default { "*/Predict", "*/Predict10", "*/Predict90" };
	
	@AttributeDefinition(name = "Debug?", description = "Use file to read prediction?")
	boolean debug() default false;
	
	@AttributeDefinition(name = "Debug File", description = "file to read prediction")
	String debug_file() default "/usr/lib/openems/forecasts.json";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Predictor Production Solcast-Model [{id}]";
}