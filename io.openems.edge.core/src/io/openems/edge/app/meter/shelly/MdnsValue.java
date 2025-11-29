package io.openems.edge.app.meter.shelly;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record MdnsValue(String name, ShellyType type) {
	/**
	 * Returns a {@link JsonSerializer} for a {@link MdnsValue}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<MdnsValue> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(MdnsValue.class, json -> {
			return new MdnsValue(//
					json.getString("name"), //
					json.getEnum("type", ShellyType.class) //
			);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", obj.name()) //
					.addProperty("type", obj.type()) //
					.build();
		});
	}
}