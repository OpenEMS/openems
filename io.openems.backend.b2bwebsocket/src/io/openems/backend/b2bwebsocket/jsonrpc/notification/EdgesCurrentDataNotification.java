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
		super(METHOD);
	}

	public void addValue(String edgeId, ChannelAddress channel, JsonElement value) {
		this.values.put(edgeId, channel, value);
	}

	@Override
	public JsonObject getParams() {
		JsonObject j = new JsonObject();
		for (Entry<String, Map<ChannelAddress, JsonElement>> row : this.values.rowMap().entrySet()) {
			String edgeId = row.getKey();
			Map<ChannelAddress, JsonElement> columns = row.getValue();
			JsonObject jEdge = new JsonObject();
			for (Entry<ChannelAddress, JsonElement> column : columns.entrySet()) {
				ChannelAddress channel = column.getKey();
				JsonElement value = column.getValue();
				jEdge.add(channel.toString(), value);
			}
			j.add(edgeId, jEdge);
		}
		return j;
	}

}
