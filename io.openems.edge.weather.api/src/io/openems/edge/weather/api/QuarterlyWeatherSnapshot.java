package io.openems.edge.weather.api;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;

import java.time.ZonedDateTime;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public record QuarterlyWeatherSnapshot(//
		ZonedDateTime datetime, //
		double globalHorizontalIrradiance, //
		double directNormalIrradiance) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link QuarterlyWeatherSnapshot}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<QuarterlyWeatherSnapshot> serializer() {
		return jsonObjectSerializer(QuarterlyWeatherSnapshot.class, json -> {
			return new QuarterlyWeatherSnapshot(//
					json.getZonedDateTime("datetime"), //
					json.getDouble("globalHorizontalIrradiance"), //
					json.getDouble("directNormalIrradiance"));
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("datetime", obj.datetime().toOffsetDateTime().toString())//
					.addProperty("globalHorizontalIrradiance", obj.globalHorizontalIrradiance())//
					.addProperty("directNormalIrradiance", obj.globalHorizontalIrradiance())//
					.build();
		});
	}

}
