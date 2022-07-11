package io.openems.backend.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'getEdgeConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "getEdgeConfig",
 *   "params": {
 *     edgeId: String
 *   }
 * }
 * </pre>
 */
public class GetAlertingConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "getAlertingConfig";

	/**
	 * Create {@link GetAlertingConfigRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link GetAlertingConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static GetAlertingConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new GetAlertingConfigRequest(r);
	}

	private final String edgeId;

	private GetAlertingConfigRequest(JsonrpcRequest request) {
		super(request, GetAlertingConfigRequest.METHOD);
		this.edgeId = request.getParams().get("edgeId").getAsString();
	}

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
