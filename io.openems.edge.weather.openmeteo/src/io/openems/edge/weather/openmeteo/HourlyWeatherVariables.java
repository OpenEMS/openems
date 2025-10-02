package io.openems.edge.weather.openmeteo;

public final class HourlyWeatherVariables {

	public static final String JSON_KEY = "hourly";

	public static final String WEATHER_CODE = "weather_code";
	public static final String TEMPERATURE_2M = "temperature_2m";
	public static final String IS_DAY = "is_day";

	public static final String[] ALL = { WEATHER_CODE, TEMPERATURE_2M, IS_DAY };

	private HourlyWeatherVariables() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
