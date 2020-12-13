package io.openems.edge.core.predictormanager;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.types.ChannelAddress;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request to query a 24 Hours Prediction.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "get24HoursPrediction",
 *   "params": {
 *   	"channels": string[]
 *   }
 * }
 * </pre>
 */
public class Get24HoursPredictionRequest extends JsonrpcRequest {

	public static final String METHOD = "get24HoursPrediction";

	public static Get24HoursPredictionRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		JsonArray cs = JsonUtils.getAsJsonArray(p, "channels");
		List<ChannelAddress> channels = new ArrayList<>();
		for (JsonElement c : cs) {
			channels.add(ChannelAddress.fromString(JsonUtils.getAsString(c)));
		}
		return new Get24HoursPredictionRequest(r.getId(), channels);
	}

	private final List<ChannelAddress> channels;

	public Get24HoursPredictionRequest(List<ChannelAddress> channels) {
		this(UUID.randomUUID(), channels);
	}

	public Get24HoursPredictionRequest(UUID id, List<ChannelAddress> channels) {
		super(id, METHOD);
		this.channels = channels;
	}

	@Override
	public JsonObject getParams() {
		JsonArray channels = new JsonArray();
		for (ChannelAddress channel : this.channels) {
			channels.add(channel.toString());
		}
		return JsonUtils.buildJsonObject() //
				.add("channels", channels) //
				.build();
	}

	public List<ChannelAddress> getChannels() {
		return this.channels;
	}

}
