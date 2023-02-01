package io.openems.common.jsonrpc.notification;

import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;

/**
 * Represents a JSON-RPC Notification for sending the current data of all
 * subscribed Channels.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "currentData",
 *   "params": {
 *     [channelAddress]: string | number
 *   }
 * }
 * </pre>
 */
public class CurrentDataNotification extends JsonrpcNotification {

	public static final String METHOD = "currentData";

	private final Map<String, JsonElement> data;

	public CurrentDataNotification(Map<String, JsonElement> data) {
		super(CurrentDataNotification.METHOD);
		this.data = data;
	}

	@Override
	public JsonObject getParams() {
		var p = new JsonObject();
		for (Entry<String, JsonElement> entry : this.data.entrySet()) {
			p.add(entry.getKey(), entry.getValue());
		}
		return p;
	}

}
