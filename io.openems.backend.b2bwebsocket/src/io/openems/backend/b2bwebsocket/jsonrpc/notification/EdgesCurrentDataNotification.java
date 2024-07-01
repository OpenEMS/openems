package io.openems.backend.b2bwebsocket.jsonrpc.notification;

import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcNotification;
import io.openems.common.types.ChannelAddress;

/**
 * Represents a JSON-RPC Notification for sending the current data of all
 * subscribed Channels of multiple Edges.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "method": "edgesCurrentData",
 *   "params": {
 *     [Edge-ID: string]: [{
 *       [Channel-Address: string]: number
 *     }]
 *   }
 * }
 * </pre>
 */
public class EdgesCurrentDataNotification extends JsonrpcNotification {

	public static final String METHOD = "edgesCurrentData";

	private final Table<String, ChannelAddress, JsonElement> values = HashBasedTable.create();

	public EdgesCurrentDataNotification() {
		super(EdgesCurrentDataNotification.METHOD);
	}

	/**
	 * Adds a value to the notification.
	 *
	 * @param edgeId  the Edge-ID
	 * @param channel the {@link ChannelAddress}
	 * @param value   the value
	 */
	public void addValue(String edgeId, ChannelAddress channel, JsonElement value) {
		this.values.put(edgeId, channel, value);
	}

	@Override
	public JsonObject getParams() {
		var j = new JsonObject();
		for (Entry<String, Map<ChannelAddress, JsonElement>> row : this.values.rowMap().entrySet()) {
			var edgeId = row.getKey();
			var columns = row.getValue();
			var jEdge = new JsonObject();
			for (Entry<ChannelAddress, JsonElement> column : columns.entrySet()) {
				var channel = column.getKey();
				var value = column.getValue();
				jEdge.add(channel.toString(), value);
			}
			j.add(edgeId, jEdge);
		}
		return j;
	}

}
