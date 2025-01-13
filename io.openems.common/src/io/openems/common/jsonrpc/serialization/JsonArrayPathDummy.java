package io.openems.common.jsonrpc.serialization;

import static java.util.Collections.emptyList;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.utils.JsonUtils;

public class JsonArrayPathDummy implements JsonArrayPath, JsonPathDummy {

	private JsonPathDummy elementType;

	@Override
	public <T> List<T> getAsList(Function<JsonElementPath, T> mapper) {
		final var path = new JsonElementPathDummy();
		mapper.apply(path);
		this.elementType = path;
		return emptyList();
	}

	@Override
	public JsonArray get() {
		return new JsonArray();
	}

	@Override
	public JsonElement buildPath() {
		return JsonUtils.buildJsonObject() //
				.addProperty("type", "array") //
				.onlyIf(this.elementType != null, t -> t.add("elementType", this.elementType.buildPath())) //
				.build();
	}

}
