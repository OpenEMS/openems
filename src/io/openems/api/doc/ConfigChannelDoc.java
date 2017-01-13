package io.openems.api.doc;

import com.google.gson.JsonObject;

public class ConfigChannelDoc {
	private final String name;
	private final String title;
	private final Class<?> type;
	private final boolean optional;

	public ConfigChannelDoc(String name, String title, Class<?> type, boolean optional) {
		this.name = name;
		this.title = title;
		this.type = type;
		this.optional = optional;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("name", name);
		j.addProperty("title", title);
		j.addProperty("type", type.getSimpleName());
		j.addProperty("optional", optional);
		return j;
	}
}
