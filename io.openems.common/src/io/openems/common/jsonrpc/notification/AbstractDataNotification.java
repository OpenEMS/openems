package io.openems.common.jsonrpc.notification;

import java.util.Map;

import com.google.common.collect.TreeBasedTable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Notification for timestamped or aggregated data sent
 * from Edge to Backend.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "timestampedData | aggregatedData",
 *   "params": {
 *     [timestamp: epoch in milliseconds]: {
 *       [channelAddress]: T
 *     }
 *   }
 * }
 * </pre>
 */
// TODO change to sealed class
public abstract class AbstractDataNotification extends JsonrpcNotification {

	private final TreeBasedTable<Long, String, JsonElement> data;

	protected static TreeBasedTable<Long, String, JsonElement> parseParams(//
			final JsonObject params //
	) throws OpenemsNamedException {
		var data = TreeBasedTable.<Long, String, JsonElement>create();
		for (var e1 : params.entrySet()) {
			var timestamp = Long.parseLong(e1.getKey());
			var jTime = JsonUtils.getAsJsonObject(e1.getValue());
			for (var e2 : jTime.entrySet()) {
				data.put(timestamp, e2.getKey(), e2.getValue());
			}
		}
		return data;
	}

	protected AbstractDataNotification(String method, TreeBasedTable<Long, String, JsonElement> data) {
		super(method);
		this.data = data;
	}

	/**
	 * Add timestamped data.
	 *
	 * @param timestamp the timestamp epoch in milliseconds
	 * @param data      a map of Channel-Address to {@link JsonElement} value
	 */
	public void add(long timestamp, Map<String, JsonElement> data) {
		for (var entry : data.entrySet()) {
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
		for (var e1 : this.data.rowMap().entrySet()) {
			var jTime = new JsonObject();
			for (var e2 : e1.getValue().entrySet()) {
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
