package io.openems.edge.weather.api;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.LocalDate;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record DailyWeatherSnapshot(//
		LocalDate date, //
		int weatherCode, //
		double minTemperature, //
		double maxTemperature, //
		double sunshineDuration) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link DailyWeatherSnapshot}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<DailyWeatherSnapshot> serializer() {
		return jsonObjectSerializer(DailyWeatherSnapshot.class, json -> {
			return new DailyWeatherSnapshot(//
					json.getLocalDate("date"), //
					json.getInt("weatherCode"), //
					json.getInt("minTemperature"), //
					json.getInt("maxTemperature"), //
					json.getDouble("sunshineDuration"));
		}, obj -> {
			return JsonUtils.buildJsonObject()//
					.addProperty("date", obj.date().toString())//
					.addProperty("weatherCode", obj.weatherCode())//
					.addProperty("minTemperature", obj.minTemperature())//
					.addProperty("maxTemperature", obj.maxTemperature())//
					.addProperty("sunshineDuration", obj.sunshineDuration()).build();
		});
	}
}
