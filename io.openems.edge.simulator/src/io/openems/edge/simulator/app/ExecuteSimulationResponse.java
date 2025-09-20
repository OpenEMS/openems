package io.openems.edge.simulator.app;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;

/**
 * Represents a JSON-RPC Response for 'executeSimulation'.
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
public class ExecuteSimulationResponse extends JsonrpcResponseSuccess {

	private final SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data;

	public ExecuteSimulationResponse(SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data) {
		this(UUID.randomUUID(), data);
	}

	public ExecuteSimulationResponse(UUID id, SortedMap<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> data) {
		super(id);
		this.data = data;
	}

	@Override
	public JsonObject getResult() {
		var result = new JsonObject();

		var timestamps = new JsonArray();
		for (ZonedDateTime timestamp : this.data.keySet()) {
			timestamps.add(timestamp.format(DateTimeFormatter.ISO_INSTANT));
		}
		result.add("timestamps", timestamps);

		var data = new JsonObject();
		for (Entry<ZonedDateTime, SortedMap<ChannelAddress, JsonElement>> rowEntry : this.data.entrySet()) {
			for (Entry<ChannelAddress, JsonElement> colEntry : rowEntry.getValue().entrySet()) {
				var channelAddress = colEntry.getKey().toString();
				var value = colEntry.getValue();
				var channelValuesElement = data.get(channelAddress);
				JsonArray channelValues;
				if (channelValuesElement != null) {
					channelValues = channelValuesElement.getAsJsonArray();
				} else {
					channelValues = new JsonArray();
				}
				channelValues.add(value);
				data.add(channelAddress, channelValues);
			}
		}
		result.add("data", data);

		return result;
	}

}
