package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	public static final String METHOD = "getEdgeConfig";

	/**
	 * Create {@link GetEdgeConfigRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetEdgeConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgeConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetEdgeConfigRequest(r);
	}

	public GetEdgeConfigRequest() {
		super(GetEdgeConfigRequest.METHOD);
	}

	private GetEdgeConfigRequest(JsonrpcRequest request) {
		super(request, GetEdgeConfigRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
