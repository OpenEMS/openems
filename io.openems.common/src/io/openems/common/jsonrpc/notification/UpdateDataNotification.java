package io.openems.common.jsonrpc.notification;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.ChannelAddress;

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
public class UpdateDataNotification extends JsonrpcNotification {

	public final static String METHOD = "updateData";

	private final Map<ChannelAddress, JsonElement> data = new HashMap<>();

	public UpdateDataNotification() {
		super(METHOD);
	}

	public void add(ChannelAddress channel, JsonElement value) {
		this.data.put(channel, value);
	}

	@Override
	public JsonObject getParams() {
		JsonObject p = new JsonObject();
		for (Entry<ChannelAddress, JsonElement> entry : data.entrySet()) {
			p.add(entry.getKey().toString(), entry.getValue());
		}
		return p;
	}

}
