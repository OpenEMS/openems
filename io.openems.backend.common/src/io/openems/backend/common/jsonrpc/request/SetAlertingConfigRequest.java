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
 *     timeToWait: int
 *   }
 * }
 * </pre>
 */
public class SetAlertingConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "setAlertingConfig";

	/**
	 * Create {@link SetAlertingConfigRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link SetAlertingConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static SetAlertingConfigRequest from(JsonrpcRequest r) throws OpenemsException {
		return new SetAlertingConfigRequest(r);
	}

	private final String edgeId;
	private final int timeToWait;

	private SetAlertingConfigRequest(JsonrpcRequest request) {
		super(request, SetAlertingConfigRequest.METHOD);
		this.edgeId = request.getParams().get("edgeId").getAsString();
		this.timeToWait = request.getParams().get("timeToWait").getAsInt();
	}

	public String getEdgeId() {
		return this.edgeId;
	}

	public int getTimeToWait() {
		return this.timeToWait;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("edgeId", this.edgeId) //
				.addProperty("timeToWait", this.timeToWait) //
				.build(); //
	}

}
