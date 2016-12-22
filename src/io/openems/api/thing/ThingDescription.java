package io.openems.api.thing;

import com.google.gson.JsonObject;

public class ThingDescription {

	private final String title;
	private final String text;
	private Class<? extends Thing> clazz = null;

	public ThingDescription(String title, String text) {
		this.title = title;
		this.text = text;
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	public void setClass(Class<? extends Thing> clazz) {
		this.clazz = clazz;
	}

	public Class<? extends Thing> getClazz() {
		return clazz;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("class", getClazz() != null ? getClazz().getName() : "");
		j.addProperty("title", getTitle());
		j.addProperty("text", getText());
		return j;
	}
}
