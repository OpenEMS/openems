package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsRuntimeException;

public class JsonObjectPathActual implements JsonObjectPath {
	private final JsonObject object;

	public JsonObjectPathActual(JsonElement object) {
		if (!object.isJsonObject()) {
			throw new OpenemsRuntimeException(object + " is not a JsonObject!");
		}
		this.object = object.getAsJsonObject();
	}

	@Override
	public JsonElementPath getJsonElementPath(String member) {
		return new JsonElementPathActual(this.object.get(member));
	}

	@Override
	public JsonObjectPath getJsonObjectPath(String member) {
		return new JsonObjectPathActual(this.object.get(member));
	}

	@Override
	public JsonObject get() {
		return this.object;
	}

}