package io.openems.edge.io.shelly.shellyplugsbase;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record DeviceInfo(String name, String id, String mac, int slot, String model, int gen, String fwId, String ver,
		String app, boolean authEn, String authDomain, boolean matter) {

	/**
	 * Returns a {@link JsonSerializer} for a {@link DeviceInfo}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<DeviceInfo> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(DeviceInfo.class, json -> {
			return new DeviceInfo(//
					json.getStringOrNull("name"), //
					json.getString("id"), //
					json.getString("mac"), //
					json.getInt("slot"), //
					json.getString("model"), //
					json.getInt("gen"), //
					json.getString("fw_id"), //
					json.getString("ver"), //
					json.getString("app"), //
					json.getBoolean("auth_en"), //
					json.getStringOrNull("auth_domain"), //
					json.getBoolean("matter") //
			);
		}, obj -> {
			return JsonUtils.buildJsonObject() //
					.addPropertyIfNotNull("name", obj.name()) //
					.addProperty("id", obj.id()) //
					.addProperty("mac", obj.mac()) //
					.addProperty("slot", obj.slot()) //
					.addProperty("model", obj.model()) //
					.addProperty("gen", obj.gen()) //
					.addProperty("fw_id", obj.fwId()) //
					.addProperty("ver", obj.ver()) //
					.addProperty("app", obj.app()) //
					.addProperty("auth_en", obj.authEn()) //
					.addPropertyIfNotNull("auth_domain", obj.authDomain()) //
					.addProperty("matter", obj.matter()) //
					.build();
		});
	}

}
