package io.openems.edge.common.jsonapi;

import org.json.JSONObject;

import io.openems.common.websocket.JsonrpcRequest;

public class ComponentJsonApi extends JsonrpcRequest {

	public final static String METHOD = "componentJsonApi";

	public static ComponentJsonApi from(String json) {
		return ComponentJsonApi.from(JsonrpcRequest.from(json));
	}

	public static ComponentJsonApi from(JsonrpcRequest r) {
		JSONObject j = r.getParams();
		String componentId = j.getString("componentId");
		JsonrpcRequest payload = JsonrpcRequest.from(j.getJSONObject("payload"));
		return new ComponentJsonApi(r.getId(), componentId, payload);
	}

	private final String componentId;
	private final JsonrpcRequest payload;

	public ComponentJsonApi(String id, String componentId, JsonrpcRequest payload) {
		super(id, METHOD, //
				new JSONObject() //
						.put("componentId", componentId) //
						.put("payload", payload.toJsonObject()) //
		);
		this.componentId = componentId;
		this.payload = payload;
	}

	public String getComponentId() {
		return componentId;
	}

	public JsonrpcRequest getPayload() {
		return payload;
	}

}
