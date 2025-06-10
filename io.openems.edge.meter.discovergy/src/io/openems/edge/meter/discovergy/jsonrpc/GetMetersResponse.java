package io.openems.edge.meter.discovergy.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Response for 'getMeters'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     "meters": [
 *     	 {@link DiscovergyMeter#toJson()}
 *     ]
 *   }
 * }
 * </pre>
 */
public class GetMetersResponse extends JsonrpcResponseSuccess {

	private final JsonArray meters;

	public GetMetersResponse(JsonArray meters) {
		this(UUID.randomUUID(), meters);
	}

	public GetMetersResponse(UUID id, JsonArray meters) {
		super(id);
		this.meters = meters;
	}

	@Override
	public JsonObject getResult() {
		return JsonUtils.buildJsonObject() //
				.add("meters", this.meters) //
				.build();
	}

}
