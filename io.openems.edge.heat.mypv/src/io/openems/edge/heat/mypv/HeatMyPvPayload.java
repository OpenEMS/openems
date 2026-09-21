package io.openems.edge.heat.mypv;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

/**
 * Payload for JSCalendar tasks of Heat MyPv.
 */
public record HeatMyPvPayload(Mode mode) {

	/**
	 * Returns a {@link JsonSerializer} for {@link HeatMyPvPayload}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<HeatMyPvPayload> serializer() {
		return jsonObjectSerializer(HeatMyPvPayload.class, //
				json -> new HeatMyPvPayload(//
						json.getEnum("mode", Mode.class)),
				obj -> buildJsonObject() //
						.addProperty("mode", obj.mode()) //
						.build());
	}
}
