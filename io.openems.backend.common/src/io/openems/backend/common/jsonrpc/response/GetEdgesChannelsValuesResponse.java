package io.openems.backend.common.jsonrpc.response;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.backend.common.jsonrpc.request.GetEdgesChannelsValuesRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;

/**
 * Represents a JSON-RPC Response for {@link GetEdgesChannelsValuesRequest}.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     [Edge-ID: string]: [{
 *       [Channel-Address: string]: number
 *     }]
 *   }
 * }
 * </pre>
 */
public class GetEdgesChannelsValuesResponse extends JsonrpcResponseSuccess {

	private final Table<String, ChannelAddress, JsonElement> values = HashBasedTable.create();

	public GetEdgesChannelsValuesResponse() {
		this(UUID.randomUUID());
	}

	public GetEdgesChannelsValuesResponse(UUID id) {
		super(id);
	}

	/**
	 * Adds a Value to the JSON-RPC Response.
	 *
	 * @param edgeId  the Edge-ID
	 * @param channel the {@link ChannelAddress}
	 * @param value   the value
	 */
	public void addValue(String edgeId, ChannelAddress channel, JsonElement value) {
		this.values.put(edgeId, channel, value);
	}

	@Override
	public JsonObject getResult() {
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
