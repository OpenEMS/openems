package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgeConfig",
 *   "params": {}
 * }
 * </pre>
 */
public class GetEdgeConfigRequest extends JsonrpcRequest {

	public static GetEdgeConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetEdgeConfigRequest(r.getId());
	}

	public final static String METHOD = "getEdgeConfig";

	public GetEdgeConfigRequest() {
		this(UUID.randomUUID());
	}

	public GetEdgeConfigRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
