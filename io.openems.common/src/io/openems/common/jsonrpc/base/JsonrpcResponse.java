package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcResponse extends JsonrpcMessage {

	public static JsonrpcResponse from(String json) throws OpenemsException {
		return from(JsonUtils.parseToJsonObject(json));
	}

	public static JsonrpcResponse from(JsonObject j) throws OpenemsException {
		UUID id = UUID.fromString(JsonUtils.getAsString(j, "id"));
		if (j.has("result")) {
			return new GenericJsonrpcResponseSuccess(id, JsonUtils.getAsJsonObject(j, "result"));
		} else if (j.has("error")) {
			return new GenericJsonrpcResponseError(id, JsonUtils.getAsJsonObject(j, "error"));
		}
		throw new OpenemsException("Unable to parse JsonrpcResponse");
	}

	protected JsonrpcResponse(UUID id) {
		super(id);
	}

}