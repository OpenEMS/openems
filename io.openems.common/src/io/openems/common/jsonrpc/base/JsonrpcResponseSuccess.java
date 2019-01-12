package io.openems.common.jsonrpc.base;

import java.util.UUID;

import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcResponseSuccess extends JsonrpcResponse {

	public JsonrpcResponseSuccess(UUID id) {
		super(id);
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.add("result", this.getResult()) //
				.build();
	}

	public abstract JsonObject getResult();

}