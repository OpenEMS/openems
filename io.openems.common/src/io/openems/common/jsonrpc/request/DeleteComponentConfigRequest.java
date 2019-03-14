package io.openems.common.jsonrpc.request;

import java.util.UUID;

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

	public static DeleteComponentConfigRequest from(JsonrpcRequest r) throws OpenemsNamedException {
		JsonObject p = r.getParams();
		String componentId = JsonUtils.getAsString(p, "componentId");
		return new DeleteComponentConfigRequest(r.getId(), componentId);
	}

	public final static String METHOD = "deleteComponentConfig";

	private final String componentId;

	public DeleteComponentConfigRequest(String componentId) {
		this(UUID.randomUUID(), componentId);
	}

	public DeleteComponentConfigRequest(UUID id, String componentId) {
		super(id, METHOD);
		this.componentId = componentId;
	}

	@Override
	public JsonObject getParams() {
		return JsonUtils.buildJsonObject() //
				.addProperty("componentId", this.componentId) //
				.build();
	}

	public String getComponentId() {
		return componentId;
	}
}
