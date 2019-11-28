package io.openems.edge.meter.discovergy.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getMeters'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getMeters",
 *   "params": {}
 * }
 * </pre>
 */
public class GetMetersRequest extends JsonrpcRequest {

	public static GetMetersRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetMetersRequest(r.getId());
	}

	public static final String METHOD = "getMeters";

	public GetMetersRequest() {
		this(UUID.randomUUID());
	}

	public GetMetersRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
