package io.openems.edge.weather.openmeteo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableSortedMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.edge.weather.api.WeatherData;
import io.openems.edge.weather.api.WeatherSnapshot;

public class Utils {
	/**
	 * Parses weather data from a JSON element and maps it to a sorted map of
	 * weather snapshots.
	 * 
	 * @param jsonElement      The JSON element containing weather data.
	 * @param weatherVariables An array of weather variables to be extracted from
	 *                         the JSON data.
	 * @param zone             The desired time zone for the weather data.
	 * @return A WeatherData object containing the parsed and sorted weather data.
	 */
	public static WeatherData parseWeatherDataFromJson(JsonElement jsonElement, String[] weatherVariables,
			ZoneId zone) {
		var jsonObject = jsonElement.getAsJsonObject();
		var jsonDataObject = jsonObject.getAsJsonObject("minutely_15");

		var timeStamps = jsonDataObject.getAsJsonArray("time");

		Map<String, JsonArray> variableDataMap = new HashMap<>();
		for (String variable : weatherVariables) {
			variableDataMap.put(variable, jsonDataObject.getAsJsonArray(variable));
		}

		var responseZone = ZoneId.of(jsonObject.get("timezone").getAsString());
		var result = ImmutableSortedMap.<ZonedDateTime, WeatherSnapshot>naturalOrder();

		for (int i = 0; i < timeStamps.size(); i++) {
			var timestamp = ZonedDateTime.of(LocalDateTime.parse(timeStamps.get(i).getAsString()), responseZone)
					.withZoneSameInstant(zone);

			var weatherSnapshot = new WeatherSnapshot(//
					variableDataMap.get("shortwave_radiation").get(i).getAsDouble(), //
					variableDataMap.get("direct_normal_irradiance").get(i).getAsDouble(), //
					variableDataMap.get("temperature_2m").get(i).getAsDouble(), //
					variableDataMap.get("weather_code").get(i).getAsInt()//
			);

			result.put(timestamp, weatherSnapshot);
		}

		return WeatherData.from(result.build());
	}
}
