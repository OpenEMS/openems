package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getUserAlertingConfigs",
 *   "params": {
 *      edgeId: string,
 *   }
 * }
 * </pre>
 */
public class GetUserAlertingConfigsRequest extends JsonrpcRequest {

	public static final String METHOD = "getUserAlertingConfigs";

	/**
	 * Create {@link GetUserAlertingConfigsRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetUserAlertingConfigsRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetUserAlertingConfigsRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		return new GetUserAlertingConfigsRequest(r);
	}

	private final String edgeId;

	private GetUserAlertingConfigsRequest(JsonrpcRequest request) throws OpenemsNamedException {
		super(request, GetUserAlertingConfigsRequest.METHOD);
		this.edgeId = JsonUtils.getAsString(request.getParams(), "edgeId");
	}

	/**
	 * Get the Edge-ID.
	 *
	 * @return the Edge-ID
	 */
	public String getEdgeId() {
		return this.edgeId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.build(); //
	}

}
