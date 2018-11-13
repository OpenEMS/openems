package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcResponseError extends JsonrpcResponse {

	public JsonrpcResponseError(UUID id) {
		super(id);
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.add("error", this.getError()) //
				.build();
	}

	public abstract JsonElement getError();

}