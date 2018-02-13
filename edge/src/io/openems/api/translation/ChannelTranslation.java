package io.openems.api.translation;

import java.util.HashMap;
import java.util.Locale;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class ChannelTranslation {

	private final String channelId;
	private final HashMap<Locale, String> channelNameTranslations;
	private final HashMap<Locale, String> channelDescriptionTranslations;

	public ChannelTranslation(String channelId,String defaultChannelName, String defaultChannelDescription) throws TranslationException {
		this.channelId = channelId;
		this.channelNameTranslations = new HashMap<>();
		this.channelDescriptionTranslations = new HashMap<>();
		setChannelNameTranslation(Locale.ENGLISH, defaultChannelName);
		setChannelDescriptionTranslation(Locale.ENGLISH, defaultChannelDescription);
	}

	public String getId() {
		return this.channelId;
	}

	public void setChannelNameTranslation(Locale locale, String translation) throws TranslationException {
		if (locale != null && translation != null && translation.length() > 0) {
			this.channelNameTranslations.put(locale, translation);
		} else {
			throw new TranslationException("The Locale or translation is Empty!");
		}
	}

	public void setChannelDescriptionTranslation(Locale locale, String translation) throws TranslationException {
		if (locale != null && translation != null && translation.length() > 0) {
			this.channelDescriptionTranslations.put(locale, translation);
		} else {
			throw new TranslationException("The Locale or translation is Empty!");
		}
	}

	public String getChannelName(Locale locale) {
		if(this.channelNameTranslations.containsKey(locale)) {
			return this.channelNameTranslations.get(locale);
		}
		return this.channelNameTranslations.get(Locale.ENGLISH);
	}

	public String getChannelDescription(Locale locale) {
		if(this.channelDescriptionTranslations.containsKey(locale)) {
			return this.channelDescriptionTranslations.get(locale);
		}
		return this.channelDescriptionTranslations.get(Locale.ENGLISH);
	}

	public JsonElement getAsJson(Locale locale) {
		JsonObject translation = new JsonObject();
		translation.addProperty("name", this.channelNameTranslations.get(locale));
		translation.addProperty("description", this.channelDescriptionTranslations.get(locale));
		return translation;
	}
}
