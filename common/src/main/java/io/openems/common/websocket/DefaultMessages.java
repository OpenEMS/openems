package io.openems.common.websocket;

import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.types.Device;

public class DefaultMessages {

	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "allow",
	 *			token: String
	 *		}, metadata: {
	 *			devices: [{
	 *				name: String,
	 *				role: "admin" | "installer" | "owner" | "guest",
	 *				online: boolean
	 *			}]
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @param token
	 * @return
	 */
	public static JsonObject connectionSuccessfulReply(String token, List<Device> devices) {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "allow");
		jAuthenticate.addProperty("token", token);
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		JsonObject jMetadata = new JsonObject();
		if(!devices.isEmpty()) {
			JsonArray jDevices = new JsonArray();
			for(Device device : devices) {
				JsonObject jDevice = new JsonObject();
				jDevice.addProperty("name", device.getName());
				jDevice.addProperty("role", device.getRole().toString());
				jDevice.addProperty("online", device.isOnline());
				jDevices.add(jDevice);
			}
			jMetadata.add("devices", jDevices);
		}
		j.add("metadata", jMetadata);
		return j;
	}
	
	/**
	 * <pre>
	 *	{
	 *		authenticate: {
	 *			mode: "deny"
	 *		}
	 *	}
	 * </pre>
	 * 
	 * @param token
	 * @return
	 */
	public static JsonObject connectionFailedReply() {
		JsonObject jAuthenticate = new JsonObject();
		jAuthenticate.addProperty("mode", "deny");
		JsonObject j = new JsonObject();
		j.add("authenticate", jAuthenticate);
		return j;
	}
}
