package io.openems.edge.meter.discovergy.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getMeters'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getMeters",
 *   "params": {
 *     "meterId": String
 *   }
 * }
 * </pre>
 */
public class GetFieldNamesRequest extends JsonrpcRequest {

	public static GetFieldNamesRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		String meterId = JsonUtils.getAsString(r.getParams(), "meterId");
		return new GetFieldNamesRequest(r.getId(), meterId);
	}

	public final static String METHOD = "getFieldNames";

	private final String meterId;

	public GetFieldNamesRequest(String meterId) {
		this(UUID.randomUUID(), meterId);
	}

	public GetFieldNamesRequest(UUID id, String meterId) {
		super(id, METHOD);
		this.meterId = meterId;
	}

	public String getMeterId() {
		return this.meterId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("meterId", this.meterId) //
				.build();
	}

}
