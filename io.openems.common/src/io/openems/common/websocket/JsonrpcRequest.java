package io.openems.common.websocket;

import org.json.JSONException;
import org.json.JSONObject;

public class JsonrpcRequest extends JsonrpcMessage {

	public static JsonrpcRequest from(String json) throws JSONException {
		return from(new JSONObject(json));
	}

	public static JsonrpcRequest from(JSONObject j) throws JSONException {
		String id = j.getString("id");
		String method = j.getString("method");
		JSONObject params = j.getJSONObject("params");
		return new JsonrpcRequest(id, method, params);
	}

	private final String method;
	private final JSONObject params;

	public JsonrpcRequest(String id, String method, JSONObject params) {
		super(id);
		this.method = method;
		this.params = params;
	}

	public String getMethod() {
		return method;
	}

	public JSONObject getParams() {
		return params;
	}

	@Override
	public JSONObject toJsonObject() {
		return super._toJsonObject() //
				.put("method", this.method) //
				.put("params", this.params);
	}
}