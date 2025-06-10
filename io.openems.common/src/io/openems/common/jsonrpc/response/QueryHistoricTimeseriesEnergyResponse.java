package io.openems.common.jsonrpc.response;

import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

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
 *       [channelAddress]: number | null
 *     }
 *   }
 * }
 * </pre>
 */

public class QueryHistoricTimeseriesEnergyResponse extends JsonrpcResponseSuccess {

	private final Map<ChannelAddress, JsonElement> data;

	public QueryHistoricTimeseriesEnergyResponse(Map<ChannelAddress, JsonElement> data) {
		this(UUID.randomUUID(), data);
	}

	public QueryHistoricTimeseriesEnergyResponse(UUID id, Map<ChannelAddress, JsonElement> data) {
		super(id);
		this.data = data;
	}

	@Override
	public JsonObject getResult() {
		var result = new JsonObject();
		var p = new JsonObject();

		for (Entry<ChannelAddress, JsonElement> entry : this.data.entrySet()) {
			p.add(entry.getKey().toString(), entry.getValue());
		}
		result.add("data", p);

		return result;
	}
}