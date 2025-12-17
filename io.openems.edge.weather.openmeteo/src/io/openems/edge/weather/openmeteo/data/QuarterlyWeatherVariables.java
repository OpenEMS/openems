package io.openems.edge.weather.openmeteo.data;

public final class QuarterlyWeatherVariables {

	public static final String JSON_KEY = "minutely_15";

	public static final String SHORTWAVE_RADIATION = "shortwave_radiation";
	public static final String DIRECT_RADIATION = "direct_radiation";
	public static final String DIRECT_NORMAL_IRRADIANCE = "direct_normal_irradiance";
	public static final String DIFFUSE_RADIATION = "diffuse_radiation";
	public static final String TEMPERATURE = "temperature_2m";
	public static final String SNOWFALL = "snowfall";
	public static final String SNOW_DEPTH = "snow_depth";

	public static final String[] ALL = { //
			SHORTWAVE_RADIATION, //
			DIRECT_RADIATION, //
			DIRECT_NORMAL_IRRADIANCE, //
			DIFFUSE_RADIATION, //
			TEMPERATURE, //
			SNOWFALL, //
			SNOW_DEPTH, //
	};

	private QuarterlyWeatherVariables() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
