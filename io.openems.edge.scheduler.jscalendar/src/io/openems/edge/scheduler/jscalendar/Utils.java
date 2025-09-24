package io.openems.edge.scheduler.jscalendar;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import java.util.stream.Stream;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.jsonrpc.serialization.JsonElementPath;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.utils.JsonUtils;

public class Utils {

	private Utils() {
	}

	public record Payload(String[] controllerIds) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Payload}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Payload> serializer() {
			return jsonObjectSerializer(Payload.class, json -> {
				return new Payload(//
						json.getArray("controllerIds", String[]::new, JsonElementPath::getAsString));

			}, obj -> {
				return obj == null //
						? JsonNull.INSTANCE //
						: buildJsonObject() //
								.add("controllerIds", Stream.of(obj.controllerIds()) //
										.map(JsonPrimitive::new) //
										.collect(JsonUtils.toJsonArray())) //
								.build();
			});
		}
	}
}
