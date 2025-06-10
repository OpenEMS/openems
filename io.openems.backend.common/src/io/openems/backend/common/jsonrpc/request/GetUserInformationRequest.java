package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.base.JsonrpcRequest;

/**
 * Gets the User Information.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": UUID,
 *   "method": "getUserInformation",
 *   "params": {}
 * }
 * </pre>
 */
public class GetUserInformationRequest extends JsonrpcRequest {

	public static final String METHOD = "getUserInformation";

	/**
	 * Create {@link GetUserInformationRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param request the template {@link JsonrpcRequest}
	 * @return Created {@link GetUserInformationRequest}
	 */
	public static GetUserInformationRequest from(JsonrpcRequest request) {
		return new GetUserInformationRequest(request);
	}

	private GetUserInformationRequest(JsonrpcRequest request) {
		super(request, GetUserInformationRequest.METHOD);
	}

	@Override
	public JsonObject getParams() {
		return new JsonObject();
	}

}
