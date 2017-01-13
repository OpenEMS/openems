package io.openems.api.doc;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.api.thing.Thing;

public class ThingDoc {

	private final Class<? extends Thing> clazz;
	private String title = "";
	private String text = "";
	private final List<ConfigChannelDoc> configChannels = new ArrayList<>();

	public ThingDoc(Class<? extends Thing> clazz) {
		this.clazz = clazz;
	}

	public void setThingDescription(ThingInfo thing) {
		this.title = thing.value();
		this.text = thing.text();
	}

	public String getText() {
		return text;
	}

	public String getTitle() {
		return title;
	}

	public Class<? extends Thing> getClazz() {
		return clazz;
	}

	public void addConfigChannel(ConfigChannelDoc config) {
		this.configChannels.add(config);
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("class", getClazz() != null ? getClazz().getName() : "");
		j.addProperty("title", getTitle());
		j.addProperty("text", getText());
		JsonArray jChannels = new JsonArray();
		for (ConfigChannelDoc config : this.configChannels) {
			jChannels.add(config.getAsJsonObject());
		}
		j.add("channels", jChannels);
		return j;
	}
}
