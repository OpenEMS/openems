package io.openems.common.jsonrpc.serialization;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

public class JsonElementPathDummy implements JsonElementPath, JsonPathDummy {

	private JsonPathDummy dummyPath;

	@Override
	public JsonArrayPath getAsJsonArrayPath() {
		return this.withDummyPath(new JsonArrayPathDummy());
	}

	@Override
	public JsonObjectPath getAsJsonObjectPath() {
		return this.withDummyPath(new JsonObjectPathDummy());
	}

	@Override
	public StringPath getAsStringPath() {
		return this.withDummyPath(new StringPathDummy());
	}

	@Override
	public <O> O getAsObject(JsonSerializer<O> deserializer) {
		final var dummyPath = new JsonElementPathDummy();
		this.withDummyPath(dummyPath);
		return deserializer.deserializePath(dummyPath);
	}

	private <T extends JsonPathDummy> T withDummyPath(T path) {
		if (this.dummyPath != null) {
			throw new RuntimeException("Path already set");
		}
		this.dummyPath = path;
		return path;
	}

	public JsonPathDummy getDummyPath() {
		return this.dummyPath;
	}

	@Override
	public JsonElement buildPath() {
		return this.dummyPath == null ? JsonNull.INSTANCE : this.dummyPath.buildPath();
	}

}
