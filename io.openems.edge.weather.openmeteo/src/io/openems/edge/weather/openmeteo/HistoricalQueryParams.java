package io.openems.edge.weather.openmeteo;

public final class HistoricalQueryParams {

	public static final String START_DATE = "start_date";
	public static final String END_DATE = "end_date";
	public static final String API_KEY = "apikey";
	public static final String LATITUDE = "latitude";
	public static final String LONGITUDE = "longitude";
	public static final String TIMEZONE = "timezone";

	private HistoricalQueryParams() {
		throw new AssertionError("Cannot instantiate utility class");
	}
}
