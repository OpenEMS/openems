package io.openems.common.jsonrpc.response;

import java.time.ZonedDateTime;
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
 * Represents a JSON-RPC Response for 'queryHistoricTimeseriesEnergy'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "data": {
 *       "channelAdress": [
 *       value
 *       ]
 *     }
 *   }
 * }
 * </pre>
 */

public class QueryHistoricTimeseriesEnergyResponse extends JsonrpcResponseSuccess {

	public static class EdgeInfo {
		protected final boolean online;
		
		public EdgeInfo(boolean online) {
			this.online = online;
		}
	}
	
	private final Table<ZonedDateTime, ChannelAddress, JsonElement> table;
	
	public QueryHistoricTimeseriesEnergyResponse(Table<ZonedDateTime, ChannelAddress, JsonElement> table) {
		this(UUID.randomUUID(), table);
	}
	
	public QueryHistoricTimeseriesEnergyResponse(UUID id, Table<ZonedDateTime, ChannelAddress, JsonElement> table) {
		super(id);
		this.table = table;
	}
	
	@Override
	public JsonObject getResult() {
		JsonObject result = new JsonObject();
		
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
