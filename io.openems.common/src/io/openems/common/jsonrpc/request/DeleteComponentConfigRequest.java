package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Represents a JSON-RPC Request for 'deleteComponentConfig'.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "deleteComponentConfig",
 *   "params": {
 *     "componentId": string
 *   }
 * }
 * </pre>
 */
public class DeleteComponentConfigRequest extends JsonrpcRequest {

	public static final String METHOD = "deleteComponentConfig";

	/**
	 * Create {@link DeleteComponentConfigRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link DeleteComponentConfigRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static DeleteComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentId = JsonUtils.getAsString(p, "componentId");
		return new DeleteComponentConfigRequest(r, componentId);
	}

	private final String componentId;

	public DeleteComponentConfigRequest(String componentId) {
		super(DeleteComponentConfigRequest.METHOD);
		this.componentId = componentId;
	}

	private DeleteComponentConfigRequest(JsonrpcRequest request, String componentId) {
		super(request, DeleteComponentConfigRequest.METHOD);
		this.componentId = componentId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.build();
	}

	/**
	 * Gets the Component-ID.
	 *
	 * @return Component-ID
	 */
	public String getComponentId() {
		return this.componentId;
	}
}
