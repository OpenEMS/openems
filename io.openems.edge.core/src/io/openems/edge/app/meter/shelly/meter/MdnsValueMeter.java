package io.openems.edge.app.meter.shelly.meter;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record MdnsValueMeter(String name, ShellyTypeMeter type) {
	/**
	 * Returns a {@link JsonSerializer} for a {@link MdnsValueMeter}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<MdnsValueMeter> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(MdnsValueMeter.class, json -> {
			return new MdnsValueMeter(//
					json.getString("name"), //
					json.getEnum("type", ShellyTypeMeter.class) //
			);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", obj.name()) //
					.addProperty("type", obj.type()) //
					.build();
		});
	}
}