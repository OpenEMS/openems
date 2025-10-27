package io.openems.edge.weather.openmeteo;

public final class QuarterlyWeatherVariables {

	public static final String JSON_KEY = "minutely_15";
	
	public static final String SHORTWAVE_RADIATION = "shortwave_radiation";
	public static final String DIRECT_NORMAL_IRRADIANCE = "direct_normal_irradiance";

	public static final String[] ALL = { SHORTWAVE_RADIATION, DIRECT_NORMAL_IRRADIANCE };

	private QuarterlyWeatherVariables() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
