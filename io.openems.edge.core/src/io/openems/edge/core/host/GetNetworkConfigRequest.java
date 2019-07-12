package io.openems.edge.core.host;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Gets the current network configuration.
 * 
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getNetworkConfig",
 *   "params": {}
 * }
 * </pre>
 */
public class GetNetworkConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "getNetworkConfig";

	public static GetNetworkConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetNetworkConfigRequest(r.getId());
	}

	public GetNetworkConfigRequest() {
		this(UUID.randomUUID());
	}

	public GetNetworkConfigRequest(UUID id) {
		super(id, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
