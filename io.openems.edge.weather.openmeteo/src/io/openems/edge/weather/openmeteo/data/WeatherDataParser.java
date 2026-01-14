package io.openems.edge.weather.openmeteo.data;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import com.google.gson.JsonObject;

import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public interface WeatherDataParser {

	/**
	 * Parses quarterly weather data from the given API JSON block.
	 *
	 * @param apiBlock       the JSON object containing the API response block
	 * @param responseOffset the original timezone offset of the API response
	 * @param targetZone     the target timezone to which all timestamps should be
	 *                       converted
	 * @return a list of {@link QuarterlyWeatherSnapshot} containing parsed weather
	 *         data
	 */
	List<QuarterlyWeatherSnapshot> parseQuarterly(JsonObject apiBlock, ZoneOffset responseOffset, ZoneId targetZone);

	/**
	 * Parses hourly weather data from the given API JSON block.
	 *
	 * @param apiBlock       the JSON object containing the API response block
	 * @param responseOffset the original timezone offset of the API response
	 * @param targetZone     the target timezone to which all timestamps should be
	 *                       converted
	 * @return a list of {@link HourlyWeatherSnapshot} containing parsed weather
	 *         data
	 */
	List<HourlyWeatherSnapshot> parseHourly(JsonObject apiBlock, ZoneOffset responseOffset, ZoneId targetZone);

	/**
	 * Parses daily weather data from the given API JSON block.
	 *
	 * @param apiBlock the JSON object containing the API response block
	 * 
	 * @return a list of {@link DailyWeatherSnapshot} containing parsed weather data
	 */
	List<DailyWeatherSnapshot> parseDaily(JsonObject apiBlock);
}
