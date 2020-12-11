package io.openems.edge.core.predictormanager;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
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
 *   	"channel": string
 *   }
 * }
 * </pre>
 */
public class Get24HoursPredictionRequest extends JsonrpcRequest {

	public static final String METHOD = "get24HoursPrediction";

	public static Get24HoursPredictionRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String channel = JsonUtils.getAsString(p, "channel");
		return new Get24HoursPredictionRequest(r.getId(), channel);
	}

	private final String channel;

	public Get24HoursPredictionRequest(String channel) {
		this(UUID.randomUUID(), channel);
	}

	public Get24HoursPredictionRequest(UUID id, String channel) {
		super(id, METHOD);
		this.channel = channel;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("channel", this.channel) //
				.build();
	}

	public String getChannel() {
		return channel;
	}

}
