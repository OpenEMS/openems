package io.openems.edge.predictor.production.linearmodel;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;

import java.time.ZonedDateTime;
import java.util.stream.DoubleStream;

import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;

public record ModelConfigState(ZonedDateTime lastTrainedDate, double[] betas) {

	private static final String[] WEATHER_INPUT_FEATURES = { //
			"global_horizontal_irradiance", //
			"direct_normal_irradiance" //
	};
	private static final boolean INCLUDE_DAY_TIME_FEATURES = true;

	public static String[] getWeatherInputFeatures() {
		return WEATHER_INPUT_FEATURES;
	}

	public static boolean isIncludeDayTimeFeatures() {
		return INCLUDE_DAY_TIME_FEATURES;
	}

	/**
	 * Returns a JSON serializer for {@link ModelConfigState}.
	 *
	 * @return the ModelConfigState JSON serializer
	 */
	public static JsonSerializer<ModelConfigState> serializer() {
		return jsonObjectSerializer(json -> {
			return new ModelConfigState(//
					json.getZonedDateTime("lastTrainedDate"), //
					json.getList("betas", JsonElementPath::getAsDouble).stream() //
							.mapToDouble(Double::doubleValue)//
							.toArray());
		}, obj -> buildJsonObject()//
				.addProperty("lastTrainedDate", obj.lastTrainedDate())//
				.add("betas", DoubleStream.of(obj.betas())//
						.mapToObj(JsonPrimitive::new)//
						.collect(toJsonArray()))//
				.build());
	}
}
