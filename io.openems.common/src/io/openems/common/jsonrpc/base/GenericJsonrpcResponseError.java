package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GenericJsonrpcResponseError extends JsonrpcResponseError {

	private final JsonObject error;

	public GenericJsonrpcResponseError(UUID id, JsonObject error) {
		super(id);
		this.error = error;
	}

	@Override
	public JsonElement getError() {
		return this.error;
	}

}