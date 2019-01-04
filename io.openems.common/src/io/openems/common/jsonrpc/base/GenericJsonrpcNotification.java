package io.openems.common.jsonrpc.base;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;

public class GenericJsonrpcNotification extends JsonrpcNotification {

	public static GenericJsonrpcNotification from(String json) throws OpenemsNamedException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static GenericJsonrpcNotification from(JsonObject j) throws OpenemsNamedException {
		String method = JsonUtils.getAsString(j, "method");
		JsonObject params = JsonUtils.getAsJsonObject(j, "params");
		return new GenericJsonrpcNotification(method, params);
	}

	private final JsonObject params;

	public GenericJsonrpcNotification(String method, JsonObject params) {
		super(method);
		this.params = params;
	}

	@Override
	public JsonObject getParams() {
		return this.params;
	}

}
