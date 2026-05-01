package io.openems.edge.heat.askoma;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

/**
 * Payload for JSCalendar tasks of Heat Askoma.
 */
public record HeatAskomaPayload(Mode mode) {

	/**
	 * Returns a {@link JsonSerializer} for {@link HeatAskomaPayload}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<HeatAskomaPayload> serializer() {
		return jsonObjectSerializer(HeatAskomaPayload.class, json -> {
			return new HeatAskomaPayload(//
					json.getEnum("mode", Mode.class));
		}, obj -> {
			return buildJsonObject() //
					.addProperty("mode", obj.mode()) //
					.build();
		});
	}
}
