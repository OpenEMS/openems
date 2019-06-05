package io.openems.backend.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getEdgesStatus'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgesStatus",
 *   "params": {}
 * }
 * </pre>
 */
public class GetEdgesStatusRequest extends JsonrpcRequest {

	public static GetEdgesStatusRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetEdgesStatusRequest(r.getId());
	}

	public static final String METHOD = "getEdgesStatus";

	public GetEdgesStatusRequest() {
		this(UUID.randomUUID());
	}

	public GetEdgesStatusRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
