package io.openems.common.jsonrpc.request;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.GenericJsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.utils.JsonUtils;

/**
 * Wraps a JSON-RPC Request for an OpenEMS Component that implements JsonApi.
 *
 * <pre>
 * {
 *   "jsonrpc": "2.0",
 *   "id": "UUID",
 *   "method": "componentJsonApi",
 *   "params": {
 *     "componentId": string,
 *     "payload": {@link JsonrpcRequest}
 *   }
 * }
 * </pre>
 */
public class ComponentJsonApiRequest extends JsonrpcRequest {

	public static final String METHOD = "componentJsonApi";

	/**
	 * Create {@link ComponentJsonApiRequest} from a template
	 * {@link JsonrpcRequest}.
	 *
	 * @param r the template {@link JsonrpcRequest}
	 * @return the {@link ComponentJsonApiRequest}
	 * @throws OpenemsNamedException on parse error
	 */
	public static ComponentJsonApiRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		var p = r.getParams();
		var componentId = JsonUtils.getAsString(p, "componentId");
		JsonrpcRequest payload = GenericJsonrpcRequest.fromIgnoreId(JsonUtils.getAsJsonObject(p, "payload"));
		return new ComponentJsonApiRequest(r, componentId, payload);
	}

	private final String componentId;
	private final JsonrpcRequest payload;

	public ComponentJsonApiRequest(String componentId, JsonrpcRequest payload) {
		super(ComponentJsonApiRequest.METHOD, payload.getTimeout() /* inherit timeout from payload */);
		this.componentId = componentId;
		this.payload = payload;
	}

	private ComponentJsonApiRequest(JsonrpcRequest request, String componentId, JsonrpcRequest payload) {
		super(request, ComponentJsonApiRequest.METHOD);
		this.componentId = componentId;
		this.payload = payload;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.add("payload", this.payload.toJsonObject()) //
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

	/**
	 * Gets the Payload {@link JsonrpcRequest}.
	 *
	 * @return Payload
	 */
	public JsonrpcRequest getPayload() {
		return this.payload;
	}

}
