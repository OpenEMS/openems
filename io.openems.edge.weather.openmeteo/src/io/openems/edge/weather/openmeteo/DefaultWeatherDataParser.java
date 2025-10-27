package io.openems.edge.weather.openmeteo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.edge.weather.api.DailyWeatherSnapshot;
import io.openems.edge.weather.api.HourlyWeatherSnapshot;
import io.openems.edge.weather.api.QuarterlyWeatherSnapshot;

public class DefaultWeatherDataParser implements WeatherDataParser {

	private static final String TIME = "time";

	@Override
	public List<QuarterlyWeatherSnapshot> parseQuarterly(JsonObject apiBlock, ZoneId responseZone, ZoneId targetZone) {
		var times = toListOfString(apiBlock.getAsJsonArray(TIME));
		var ghi = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.SHORTWAVE_RADIATION));
		var dni = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.DIRECT_NORMAL_IRRADIANCE));

		var result = new ArrayList<QuarterlyWeatherSnapshot>();
		for (int i = 0; i < times.size(); i++) {
			result.add(new QuarterlyWeatherSnapshot(//
					convertTimeZone(times.get(i), responseZone, targetZone), //
					ghi.get(i), //
					dni.get(i)));
		}

		return result;
	}

	@Override
	public List<HourlyWeatherSnapshot> parseHourly(JsonObject apiBlock, ZoneId responseZone, ZoneId targetZone) {
		var times = toListOfString(apiBlock.getAsJsonArray(TIME));
		var weatherCodes = toListOfInt(apiBlock.getAsJsonArray(HourlyWeatherVariables.WEATHER_CODE));
		var temperatures = toListOfDouble(apiBlock.getAsJsonArray(HourlyWeatherVariables.TEMPERATURE_2M));
		var isDay = toListOfBooleans(apiBlock.getAsJsonArray(HourlyWeatherVariables.IS_DAY));

		var result = new ArrayList<HourlyWeatherSnapshot>();
		for (int i = 0; i < times.size(); i++) {
			result.add(new HourlyWeatherSnapshot(//
					convertTimeZone(times.get(i), responseZone, targetZone), //
					weatherCodes.get(i), //
					temperatures.get(i), //
					isDay.get(i)));
		}

		return result;
	}

	@Override
	public List<DailyWeatherSnapshot> parseDaily(JsonObject apiBlock) {
		var times = toListOfString(apiBlock.getAsJsonArray(TIME));
		var weatherCodes = toListOfInt(apiBlock.getAsJsonArray(DailyWeatherVariables.WEATHER_CODE));
		var minTemperatures = toListOfDouble(apiBlock.getAsJsonArray(DailyWeatherVariables.TEMPERATURE_2M_MIN));
		var maxTemperatures = toListOfDouble(apiBlock.getAsJsonArray(DailyWeatherVariables.TEMPERATURE_2M_MAX));
		var sunshineDurations = toListOfDouble(apiBlock.getAsJsonArray(DailyWeatherVariables.SUNSHINE_DURATION));

		var result = new ArrayList<DailyWeatherSnapshot>();
		for (int i = 0; i < times.size(); i++) {
			result.add(new DailyWeatherSnapshot(//
					LocalDate.parse(times.get(i)), //
					weatherCodes.get(i), //
					minTemperatures.get(i), //
					maxTemperatures.get(i), //
					sunshineDurations.get(i)));
		}

		return result;
	}

	private static List<String> toListOfString(JsonArray arr) {
		var list = new ArrayList<String>();
		arr.forEach(e -> list.add(e.getAsString()));
		return list;
	}

	private static List<Integer> toListOfInt(JsonArray arr) {
		var list = new ArrayList<Integer>();
		arr.forEach(e -> list.add(e.getAsInt()));
		return list;
	}

	private static List<Double> toListOfDouble(JsonArray arr) {
		var list = new ArrayList<Double>();
		arr.forEach(e -> list.add(e.getAsDouble()));
		return list;
	}

	private static List<Boolean> toListOfBooleans(JsonArray arr) {
		var list = new ArrayList<Boolean>();
		arr.forEach(e -> list.add(e.getAsInt() == 1));
		return list;
	}

	private static ZonedDateTime convertTimeZone(String isoString, ZoneId responseZone, ZoneId targetZone) {
		return LocalDateTime.parse(isoString)//
				.atZone(responseZone)//
				.withZoneSameInstant(targetZone);
	}
}
