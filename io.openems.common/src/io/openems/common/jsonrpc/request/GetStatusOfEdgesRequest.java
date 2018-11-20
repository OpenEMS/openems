package io.openems.common.jsonrpc.request;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Represents a JSON-RPC Request for 'getStatusOfEdges'.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getStatusOfEdges",
 *   "params": {}
 * }
 * </pre>
 */
public class GetStatusOfEdgesRequest extends JsonrpcRequest {

	public static GetStatusOfEdgesRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetStatusOfEdgesRequest(r.getId());
	}

	public final static String METHOD = "getStatusOfEdges";

	public GetStatusOfEdgesRequest() {
		this(UUID.randomUUID());
	}

	public GetStatusOfEdgesRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
