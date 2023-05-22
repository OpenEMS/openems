package io.openems.common.jsonrpc.notification;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
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

	/**
	 * Parses a {@link JsonrpcNotification} to a
	 * {@link TimestampedDataNotification}.
	 *
	 * @param n the {@link JsonrpcNotification}
	 * @return the {@link TimestampedDataNotification}
	 * @throws OpenemsNamedException on error
	 */
	public static TimestampedDataNotification from(JsonrpcNotification n) throws OpenemsNamedException {
		var result = new TimestampedDataNotification();
		var j = n.getParams();
		for (Entry<String, JsonElement> e1 : j.entrySet()) {
			var timestamp = Long.parseLong(e1.getKey());
			var jTime = JsonUtils.getAsJsonObject(e1.getValue());
			for (Entry<String, JsonElement> e2 : jTime.entrySet()) {
				result.add(timestamp, e2.getKey(), e2.getValue());
			}
		}
		return result;
	}

	public static final String METHOD = "timestampedData";

	private final TreeBasedTable<Long, String, JsonElement> data = TreeBasedTable.create();

	public TimestampedDataNotification() {
		super(TimestampedDataNotification.METHOD);
	}

	/**
	 * Add timestamped data.
	 *
	 * @param timestamp the timestamp epoch in milliseconds
	 * @param data      a map of Channel-Address to {@link JsonElement} value
	 */
	public void add(long timestamp, Map<String, JsonElement> data) {
		for (Entry<String, JsonElement> entry : data.entrySet()) {
			this.add(timestamp, entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Add a timestamped value.
	 *
	 * @param timestamp the timestamp epoch in milliseconds
	 * @param address   the Channel-Address
	 * @param value     the {@link JsonElement} value
	 */
	public void add(long timestamp, String address, JsonElement value) {
		this.data.put(timestamp, address, value);
	}

	@Override
	public JsonObject getParams() {
		var p = new JsonObject();
		for (Entry<Long, Map<String, JsonElement>> e1 : this.data.rowMap().entrySet()) {
			var jTime = new JsonObject();
			for (Entry<String, JsonElement> e2 : e1.getValue().entrySet()) {
				var address = e2.getKey();
				var value = e2.getValue();
				jTime.add(address, value);
			}
			p.add(e1.getKey().toString(), jTime);
		}
		return p;
	}

	public TreeBasedTable<Long, String, JsonElement> getData() {
		return this.data;
	}
}
