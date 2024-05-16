package io.openems.common.jsonrpc.serialization;

import java.util.List;
import java.util.function.Function;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsRuntimeException;
import io.openems.common.utils.JsonUtils;

public class JsonArrayPathActual implements JsonArrayPath {

	private final JsonArray object;

	public JsonArrayPathActual(JsonElement object) {
		if (!object.isJsonArray()) {
			throw new OpenemsRuntimeException(object + " is not a JsonArray!");
		}
		this.object = object.getAsJsonArray();
	}

	@Override
	public <T> List<T> getAsList(Function<JsonElementPath, T> mapper) {
		return JsonUtils.stream(this.object) //
				.map(JsonElementPathActual::new) //
				.map(mapper) //
				.toList();
	}

	@Override
	public JsonArray get() {
		return this.object;
	}

}