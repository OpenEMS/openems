package io.openems.edge.app.meter.shelly.diy;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record MdnsValueDiy(String name, ShellyTypeDiy type) {
	/**
	 * Returns a {@link JsonSerializer} for a {@link MdnsValueDiy}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<MdnsValueDiy> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(MdnsValueDiy.class, json -> {
			return new MdnsValueDiy(//
					json.getString("name"), //
					json.getEnum("type", ShellyTypeDiy.class) //
			);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addProperty("name", obj.name()) //
					.addProperty("type", obj.type()) //
					.build();
		});
	}
}