package io.openems.edge.core.host.jsonrpc;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.core.host.NetworkConfiguration;

/**
 * JSON-RPC Response to "getNetworkConfig" Request.
 *
 * <p>
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "result": {
 *     {@link NetworkConfiguration#toJson()}
 *   }
 * }
 * </pre>
 */
public class GetNetworkConfigResponse extends JsonrpcResponseSuccess {

	private final NetworkConfiguration config;

	public GetNetworkConfigResponse(UUID id, NetworkConfiguration config) {
		super(id);
		this.config = config;
	}

	@Override
	public JsonObject getResult() {
		return this.config.toJson();
	}

}
