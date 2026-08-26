package io.openems.edge.io.shelly.common.gen2;

import java.util.Arrays;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.common.jsonrpc.serialization.JsonSerializerUtil;
import io.openems.common.utils.JsonUtils;

public record Gen2RpcDeviceInfo(String name, String id, String mac, int slot, String model, int gen, String fwId,
		String ver, String app, boolean authEn, String authDomain) {

	/**
	 * Checks if the app name from the current device is contained in one of the
	 * given shelly app names.
	 *
	 * @param shellyAppNames List of possible shelly app names / device types
	 * @return true if the app name from the current device is contained in one of
	 *         the given shelly app names, false otherwise
	 */
	public boolean isDeviceType(String[] shellyAppNames) {
		return Arrays.asList(shellyAppNames).contains(this.app);
	}

	/**
	 * Returns a {@link JsonSerializer} for a {@link Gen2RpcDeviceInfo}.
	 *
	 * @return the created {@link JsonSerializer}
	 */
	public static JsonSerializer<Gen2RpcDeviceInfo> serializer() {
		return JsonSerializerUtil.jsonObjectSerializer(Gen2RpcDeviceInfo.class, json -> {
			return new Gen2RpcDeviceInfo(//
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
					json.getStringOrNull("auth_domain") //
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
					.build();
		});
	}

}
