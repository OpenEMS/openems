package io.openems.edge.weather.api;

import java.time.ZonedDateTime;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.common.type.QuarterlyValues;

public class WeatherData extends QuarterlyValues<WeatherSnapshot> {

	public static final WeatherData EMPTY_WEATHER_DATA = new WeatherData(ImmutableSortedMap.of());

	/**
	 * Creates a WeatherData object from a sorted map of weather snapshots. If the
	 * map is empty, returns an empty WeatherData object.
	 * 
	 * @param map A sorted map of ZonedDateTime keys and WeatherSnapshot values.
	 * @return A WeatherData object containing the provided map, or an empty
	 *         WeatherData if the map is empty.
	 */
	public static WeatherData from(ImmutableSortedMap<ZonedDateTime, WeatherSnapshot> map) {
		if (map.isEmpty()) {
			return EMPTY_WEATHER_DATA;
		}
		return new WeatherData(map);
	}

	/**
	 * Creates a WeatherData object from a given ZonedDateTime and one or more
	 * WeatherSnapshot values. If no snapshots are provided, returns an empty
	 * WeatherData object.
	 * 
	 * @param time   The starting time associated with the weather snapshots.
	 * @param values One or more WeatherSnapshot values to include in the
	 *               WeatherData.
	 * @return A WeatherData object containing the snapshots, or an empty
	 *         WeatherData if no values are provided.
	 */
	public static WeatherData from(ZonedDateTime time, WeatherSnapshot... values) {
		if (values.length == 0) {
			return EMPTY_WEATHER_DATA;
		}
		return new WeatherData(time, values);
	}

	private WeatherData(ImmutableSortedMap<ZonedDateTime, WeatherSnapshot> weatherDataPerQuarter) {
		super(weatherDataPerQuarter);
	}

	private WeatherData(ZonedDateTime time, WeatherSnapshot... values) {
		super(time, values);
	}

	/**
	 * Converts the WeatherData into an array of WeatherSnapshot objects.
	 * 
	 * @return An array of WeatherSnapshot objects representing the weather data.
	 */
	public WeatherSnapshot[] asArray() {
		return super.asArray(WeatherSnapshot[]::new);
	}
}
