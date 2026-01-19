package io.openems.edge.weather.api;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record HourlyWeatherSnapshot(//
		ZonedDateTime datetime, //
		int weatherCode, //
		double temperature, //
		boolean isDay) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link HourlyWeatherSnapshot}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<HourlyWeatherSnapshot> serializer() {
		return jsonObjectSerializer(HourlyWeatherSnapshot.class, json -> {
			return new HourlyWeatherSnapshot(//
					json.getZonedDateTime("datetime"), //
					json.getInt("weatherCode"), //
					json.getDouble("temperature"), //
					json.getBoolean("isDay"));
		}, obj -> {
			return JsonUtils.buildJsonObject()//
					.addProperty("datetime", obj.datetime().toOffsetDateTime().toString())//
					.addProperty("weatherCode", obj.weatherCode())//
					.addProperty("temperature", obj.temperature())//
					.addProperty("isDay", obj.isDay())//
					.build();
		});
	}
}
