package io.openems.common.jsonrpc.notification;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for timestamped data sent from Edge to
 * Backend.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "timestampedData",
 *   "params": {
 *     [timestamp: epoch in milliseconds]: {
 *       [channelAddress]: String | Number
 *     }
 *   }
 * }
 * </pre>
 */
public class TimestampedDataNotification extends JsonrpcNotification {

	public static TimestampedDataNotification from(JsonrpcNotification notification) throws OpenemsNamedException {
		TimestampedDataNotification result = new TimestampedDataNotification();
		JsonObject j = notification.getParams();
		for (Entry<String, JsonElement> e1 : j.entrySet()) {
			long timestamp = Long.parseLong(e1.getKey());
			JsonObject jTime = JsonUtils.getAsJsonObject(e1.getValue());
			for (Entry<String, JsonElement> e2 : jTime.entrySet()) {
				result.add(timestamp, ChannelAddress.fromString(e2.getKey()), e2.getValue());
			}
		}
		return result;
	}

	public static final String METHOD = "timestampedData";

	private final TreeBasedTable<Long, ChannelAddress, JsonElement> data = TreeBasedTable.create();

	public TimestampedDataNotification() {
		super(METHOD);
	}

	public void add(long timestamp, Map<ChannelAddress, JsonElement> data) {
		for (Entry<ChannelAddress, JsonElement> entry : data.entrySet()) {
			this.add(timestamp, entry.getKey(), entry.getValue());
		}
	}

	public void add(long timestamp, ChannelAddress address, JsonElement value) {
		this.data.put(timestamp, address, value);
	}

	@Override
	public JsonObject getParams() {
		JsonObject p = new JsonObject();
		for (Entry<Long, Map<ChannelAddress, JsonElement>> e1 : this.data.rowMap().entrySet()) {
			JsonObject jTime = new JsonObject();
			for (Entry<ChannelAddress, JsonElement> e2 : e1.getValue().entrySet()) {
				ChannelAddress address = e2.getKey();
				JsonElement value = e2.getValue();
				jTime.add(address.toString(), value);
			}
			p.add(e1.getKey().toString(), jTime);
		}
		return p;
	}

	public TreeBasedTable<Long, ChannelAddress, JsonElement> getData() {
		return this.data;
	}
}
