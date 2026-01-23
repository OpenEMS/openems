package io.openems.edge.weather.api;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record QuarterlyWeatherSnapshot(//
		ZonedDateTime datetime, //
		double shortwaveRadiation, //
		double directRadiation, //
		double directNormalIrradiance, //
		double diffuseRadiation, //
		double temperature, //
		double snowfall, //
		double snowDepth) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link QuarterlyWeatherSnapshot}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<QuarterlyWeatherSnapshot> serializer() {
		return jsonObjectSerializer(QuarterlyWeatherSnapshot.class, json -> {
			return new QuarterlyWeatherSnapshot(//
					json.getZonedDateTime("datetime"), //
					json.getDouble("shortwaveRadiation"), //
					json.getDouble("directRadiation"), //
					json.getDouble("directNormalIrradiance"), //
					json.getDouble("diffuseRadiation"), //
					json.getDouble("temperature"), //
					json.getDouble("snowfall"), //
					json.getDouble("snowDepth"));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("datetime", obj.datetime().toOffsetDateTime().toString())//
					.addProperty("shortwaveRadiation", obj.shortwaveRadiation())//
					.addProperty("directRadiation", obj.directRadiation())//
					.addProperty("directNormalIrradiance", obj.directNormalIrradiance())//
					.addProperty("diffuseRadiation", obj.diffuseRadiation())//
					.addProperty("temperature", obj.temperature())//
					.addProperty("snowfall", obj.snowfall())//
					.addProperty("snowDepth", obj.snowDepth())//
					.build();
		});
	}

}
