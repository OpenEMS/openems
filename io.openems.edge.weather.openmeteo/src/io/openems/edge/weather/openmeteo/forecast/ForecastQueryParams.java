package io.openems.edge.weather.openmeteo.forecast;

public final class ForecastQueryParams {

	public static final String FORECAST_DAYS = "forecast_days";
	public static final String PAST_DAYS = "past_days";
	public static final String API_KEY = "apikey";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String TIMEZONE = "timezone";
	public static final String UTC_OFFSET_SECONDS = "utc_offset_seconds";

	private ForecastQueryParams() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
