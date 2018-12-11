package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class GenericJsonrpcResponseSuccess extends JsonrpcResponseSuccess {

	private final JsonObject result;

	public GenericJsonrpcResponseSuccess(UUID id) {
		this(id, new JsonObject());
	}

	public GenericJsonrpcResponseSuccess(UUID id, JsonObject result) {
		super(id);
		this.result = result;
	}

	@Override
	public JsonElement getResult() {
		return this.result;
	}

}