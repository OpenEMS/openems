package io.openems.common.jsonrpc.response;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import com.google.common.collect.Table;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;

/**
 * Represents a JSON-RPC Response for 'queryHistoricTimeseriesData'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "timestamps": [
 *       '2011-12-03T10:15:30Z',...
 *     ],
 *     "data": {
 *       "componentId/channelId": [
 *         value1, value2,...
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */
public class QueryHistoricTimeseriesDataResponse extends JsonrpcResponseSuccess {

	public static class EdgeInfo {
		protected final boolean online;

		public EdgeInfo(boolean online) {
			this.online = online;
		}
	}

	private final Table<ZonedDateTime, ChannelAddress, JsonElement> table;

	public QueryHistoricTimeseriesDataResponse(Table<ZonedDateTime, ChannelAddress, JsonElement> table) {
		this(UUID.randomUUID(), table);
	}

	public QueryHistoricTimeseriesDataResponse(UUID id, Table<ZonedDateTime, ChannelAddress, JsonElement> table) {
		super(id);
		this.table = table;
	}

	@Override
	public JsonObject getResult() {
		JsonObject result = new JsonObject();

		JsonArray timestamps = new JsonArray();
		for (ZonedDateTime timestamp : table.rowKeySet()) {
			timestamps.add(timestamp.format(DateTimeFormatter.ISO_INSTANT));
		}
		result.add("timestamps", timestamps);

		JsonObject data = new JsonObject();
		for (Entry<ChannelAddress, Map<ZonedDateTime, JsonElement>> entry : table.columnMap().entrySet()) {
			JsonArray channelData = new JsonArray();
			for (JsonElement value : entry.getValue().values()) {
				channelData.add(value);
			}
			data.add(entry.getKey().toString(), channelData);
		}
		result.add("data", data);

		return result;
	}

}
