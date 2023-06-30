package io.openems.edge.core.host.jsonrpc;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	/**
	 * Parses a generic {@link JsonrpcRequest} to a {@link GetNetworkConfigRequest}.
	 *
	 * @param r the {@link JsonrpcRequest}
	 * @return the {@link GetNetworkConfigRequest}
	 * @throws OpenemsNamedException on error
	 */
	public static GetNetworkConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetNetworkConfigRequest(r);
	}

	public GetNetworkConfigRequest() {
		super(METHOD);
	}

	private GetNetworkConfigRequest(JsonrpcRequest request) {
		super(request, METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
