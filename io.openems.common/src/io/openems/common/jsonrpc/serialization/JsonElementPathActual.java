package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;

public class JsonElementPathActual implements JsonElementPath {
	private final JsonElement element;

	public JsonElementPathActual(JsonElement element) {
		this.element = element;
	}

	@Override
	public JsonArrayPath getAsJsonArrayPath() {
		return new JsonArrayPathActual(this.element);
	}

	@Override
	public JsonObjectPath getAsJsonObjectPath() {
		return new JsonObjectPathActual(this.element);
	}

	@Override
	public StringPath getAsStringPath() {
		return new StringPathActual(this.element);
	}

	@Override
	public <O> O getAsObject(JsonSerializer<O> deserializer) {
		return deserializer.deserializePath(new JsonElementPathActual(this.element));
	}

}
