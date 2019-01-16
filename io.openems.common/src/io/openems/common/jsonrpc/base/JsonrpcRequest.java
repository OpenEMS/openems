package io.openems.common.jsonrpc.base;

import java.util.UUID;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public abstract class JsonrpcRequest extends AbstractJsonrpcRequest {

	private final UUID id;

	public JsonrpcRequest(String method) {
		this(UUID.randomUUID(), method);
	}

	public JsonrpcRequest(UUID id, String method) {
		super(method);
		this.id = id;
	}

	public UUID getId() {
		return id;
	}

	@Override
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject(super.toJsonObject()) //
				.addProperty("id", this.getId().toString()) //
				.build();
	}
}