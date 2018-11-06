package io.openems.common.websocket;

import org.json.JSONObject;

public class JsonrpcResponseSuccess extends JsonrpcResponse {

	private final JSONObject result;

	public JsonrpcResponseSuccess(String id, JSONObject result) {
		super(id);
		this.result = result;
	}

	@Override
	public JSONObject toJsonObject() {
		return super._toJsonObject() //
				.put("result", this.result); //
	}

}