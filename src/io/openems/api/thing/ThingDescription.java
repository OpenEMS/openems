package io.openems.api.thing;

import java.lang.reflect.Member;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.core.utilities.ConfigUtils;

public class ThingDescription {

	private final String title;
	private final String text;
	private Class<? extends Thing> clazz = null;
	private List<Member> channels = null;

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
		this.channels = ConfigUtils.getChannelMembers(clazz);
	}

	public Class<? extends Thing> getClazz() {
		return clazz;
	}

	public JsonObject getAsJsonObject() {
		JsonObject j = new JsonObject();
		j.addProperty("class", getClazz() != null ? getClazz().getName() : "");
		j.addProperty("title", getTitle());
		j.addProperty("text", getText());
		JsonArray jChannels = new JsonArray();
		for (Member member : this.channels) {
			JsonObject jChannel = new JsonObject();
			jChannel.addProperty("name", member.getName());
			jChannels.add(jChannel);
		}
		j.add("channels", jChannels);
		return j;
	}
}
