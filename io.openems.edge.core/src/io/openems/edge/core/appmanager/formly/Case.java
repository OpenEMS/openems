package io.openems.edge.core.appmanager.formly;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import io.openems.common.utils.JsonUtils;

public record Case(JsonElement value, JsonElement defaultValue) {

	public Case(String value, String defaultValue) {
		this(new JsonPrimitive(value), new JsonPrimitive(defaultValue));
	}

	public Case(Number value, String defaultValue) {
		this(new JsonPrimitive(value), new JsonPrimitive(defaultValue));
	}

	/**
	 * Creates a {@link JsonObject} from this {@link Case}.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJsonObject() {
		return JsonUtils.buildJsonObject() //
				.add("case", this.value) //
				.add("defaultValue", this.defaultValue) //
				.build();
	}

}