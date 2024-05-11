package io.openems.common.jsonrpc.serialization;

import static io.openems.common.utils.JsonUtils.toJsonObject;

import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.utils.JsonUtils;

public class JsonObjectPathDummy implements JsonObjectPath, JsonPathDummy {

	private final Map<String, JsonPathDummy> paths = new TreeMap<>();

	@Override
	public JsonElementPath getJsonElementPath(String member) {
		return this.withDummyPath(member, new JsonElementPathDummy());
	}

	@Override
	public JsonObjectPath getJsonObjectPath(String member) {
		return this.withDummyPath(member, new JsonObjectPathDummy());
	}

	@Override
	public JsonObject get() {
		return new JsonObject();
	}

	@Override
	public JsonElement buildPath() {
		return JsonUtils.buildJsonObject() //
				.addProperty("type", "object") //
				.add("properties", this.paths.entrySet().stream() //
						.collect(toJsonObject(Entry::getKey, input -> input.getValue().buildPath()))) //
				.build();
	}

	private final <T extends JsonPathDummy> T withDummyPath(String member, T path) {
		this.paths.put(member, path);
		return path;
	}

}
