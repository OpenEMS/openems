package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
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

	public static final String METHOD = "getEdgesStatus";

	/**
	 * Create {@link GetEdgesStatusRequest} from a template {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetEdgesStatusRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetEdgesStatusRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetEdgesStatusRequest(r);
	}

	public GetEdgesStatusRequest() {
		super(GetEdgesStatusRequest.METHOD);
	}

	private GetEdgesStatusRequest(JsonrpcRequest request) {
		super(request, GetEdgesStatusRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
