package io.openems.edge.controller.io.heatpump.sgready;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;

public record HeatPumpPayload(BaseMode baseMode) {

	/**
	 * Returns a {@link JsonSerializer} for {@link HeatPumpPayload}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<HeatPumpPayload> serializer() {
		return jsonObjectSerializer(HeatPumpPayload.class, //
				json -> new HeatPumpPayload(json.getEnum("baseMode", BaseMode.class)), //
				obj -> buildJsonObject() //
						.addProperty("baseMode", obj.baseMode()) //
						.build());
	}
}
