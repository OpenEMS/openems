package io.openems.edge.weather.openmeteo.data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
	public List<QuarterlyWeatherSnapshot> parseQuarterly(//
			JsonObject apiBlock, //
			ZoneOffset responseOffset, //
			ZoneId targetZone) {
		var datetimes = toListOfString(apiBlock.getAsJsonArray(TIME));
		var shortwaveRadiations = toListOfDouble(
				apiBlock.getAsJsonArray(QuarterlyWeatherVariables.SHORTWAVE_RADIATION));
		var directRadiations = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.DIRECT_RADIATION));
		var directNormalIrradiances = toListOfDouble(
				apiBlock.getAsJsonArray(QuarterlyWeatherVariables.DIRECT_NORMAL_IRRADIANCE));
		var diffuseRadiation = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.DIFFUSE_RADIATION));
		var temperatures = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.TEMPERATURE));
		var snowfalls = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.SNOWFALL));
		var snowDepths = toListOfDouble(apiBlock.getAsJsonArray(QuarterlyWeatherVariables.SNOW_DEPTH));

		var result = new ArrayList<QuarterlyWeatherSnapshot>();
		for (int i = 0; i < datetimes.size(); i++) {
			result.add(new QuarterlyWeatherSnapshot(//
					convertTimeZone(datetimes.get(i), responseOffset, targetZone), //
					shortwaveRadiations.get(i), //
					directRadiations.get(i), //
					directNormalIrradiances.get(i), //
					diffuseRadiation.get(i), //
					temperatures.get(i), //
					snowfalls.get(i), //
					snowDepths.get(i)));
		}

		return result;
	}

	@Override
	public List<HourlyWeatherSnapshot> parseHourly(//
			JsonObject apiBlock, //
			ZoneOffset responseOffset, //
			ZoneId targetZone) {
		var datetimes = toListOfString(apiBlock.getAsJsonArray(TIME));
		var weatherCodes = toListOfInt(apiBlock.getAsJsonArray(HourlyWeatherVariables.WEATHER_CODE));
		var temperatures = toListOfDouble(apiBlock.getAsJsonArray(HourlyWeatherVariables.TEMPERATURE_2M));
		var isDay = toListOfBooleans(apiBlock.getAsJsonArray(HourlyWeatherVariables.IS_DAY));

		var result = new ArrayList<HourlyWeatherSnapshot>();
		for (int i = 0; i < datetimes.size(); i++) {
			result.add(new HourlyWeatherSnapshot(//
					convertTimeZone(datetimes.get(i), responseOffset, targetZone), //
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

	/**
	 * Converts an ISO date-time string to a ZonedDateTime in the target time zone.
	 * 
	 * <p>
	 * It is important to use the offset from the response because the response
	 * timestamps are provided with their specific offset. This ensures that, even
	 * if the query period spans a daylight saving time change, the times are
	 * correctly converted to the target zone.
	 *
	 * @param isoString      the ISO date-time string (without offset)
	 * @param responseOffset the offset provided with the response timestamps
	 * @param targetZone     the target time zone for conversion
	 * @return the converted ZonedDateTime in the target time zone
	 */
	private static ZonedDateTime convertTimeZone(String isoString, ZoneOffset responseOffset, ZoneId targetZone) {
		return LocalDateTime.parse(isoString)//
				.atOffset(responseOffset)//
				.toZonedDateTime()//
				.withZoneSameInstant(targetZone);
	}
}
