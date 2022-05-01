package io.openems.edge.predictor.forecastsolar;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Predictor Forecast.Solar", //
		description = "Gets predictions from https://forecast.solar")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictor0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "Channel-Address", description = "Channel-Addresses this Predictor is used for, e.g. 'pvInverter0/ActivePower'")
	String channelAddress() default "_sum/ProductionActivePower";

	@AttributeDefinition(name = "Apikey", description = "Personal API key for registered users; empty for public API")
	String apikey();

	@AttributeDefinition(name = "Latitude", description = "Latitude of location, -90 (south) ... 90 (north)")
	double latitude();

	@AttributeDefinition(name = "Longitude", description = "Longitude of location, -180 (west) … 180 (east)")
	double longitude();

	@AttributeDefinition(name = "Declination", description = "Plane declination, 0 (horizontal) … 90 (vertical)")
	int declination();

	@AttributeDefinition(name = "Azimuth", description = "Plane azimuth, -180 … 180 (-180 = north, -90 = east, 0 = south, 90 = west, 180 = north)")
	int azimuth();

	@AttributeDefinition(name = "Modules Power", description = "Installed modules power in [kWp]")
	float modulesPower();

	String webconsole_configurationFactory_nameHint() default "Predictor Forecast.Solar [{id}]";

}