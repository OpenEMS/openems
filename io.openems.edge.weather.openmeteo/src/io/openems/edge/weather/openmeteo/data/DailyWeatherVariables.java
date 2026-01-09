package io.openems.edge.weather.openmeteo.data;

public final class DailyWeatherVariables {

	public static final String JSON_KEY = "daily";

	public static final String WEATHER_CODE = "weather_code";
	public static final String TEMPERATURE_2M_MIN = "temperature_2m_min";
	public static final String TEMPERATURE_2M_MAX = "temperature_2m_max";
	public static final String SUNSHINE_DURATION = "sunshine_duration";

	public static final String[] ALL = { //
			WEATHER_CODE, //
			TEMPERATURE_2M_MIN, //
			TEMPERATURE_2M_MAX, //
			SUNSHINE_DURATION, //
	};

	private DailyWeatherVariables() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
