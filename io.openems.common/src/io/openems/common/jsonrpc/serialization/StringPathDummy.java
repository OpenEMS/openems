package io.openems.common.jsonrpc.serialization;

import java.util.UUID;

import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public class StringPathDummy implements StringPath, JsonPathDummy {

	@Override
	public String get() {
		return "";
	}

	@Override
	public UUID getAsUuid() {
		return UUID.randomUUID();
	}

	@Override
	public JsonElement buildPath() {
		return JsonUtils.buildJsonObject() //
				.addProperty("type", "string") //
				.build();
	}

}
