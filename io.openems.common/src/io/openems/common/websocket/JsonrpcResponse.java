package io.openems.common.websocket;

import org.json.JSONObject;

public class JsonrpcResponse extends JsonrpcMessage {

	public JsonrpcResponse(String id) {
		super(id);
	}

	@Override
	public JSONObject toJsonObject() {
		return super._toJsonObject();
	}

}