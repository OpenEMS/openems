package io.openems.common.jsonrpc.base;

import java.util.UUID;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcRequest extends JsonrpcMessage {

	private final String method;

	public JsonrpcRequest(String method) {
		this(UUID.randomUUID(), method);
	}

	public JsonrpcRequest(UUID id, String method) {
		super(id);
		this.method = method;
	}

	public String getMethod() {
		return method;
	}

	public abstract JsonObject getParams();

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("method", this.method) //
				.add("params", this.getParams()) //
				.build();
	}
}