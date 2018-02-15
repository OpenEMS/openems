package io.openems.api.translation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public abstract class ThingTranslation {

	private final Class<?> thingClass;
	private final HashMap<String, ChannelTranslation> channels;
	private final HashMap<Locale, String> thingNameTranslations;
	private final HashMap<Locale, String> thingDescriptionTranslations;

	public ThingTranslation(Class<?> thingClass, String defaultThingName, String defaultThingDescription) throws TranslationException {
		this.thingClass = thingClass;
		this.channels = new HashMap<>();
		this.thingNameTranslations = new HashMap<>();
		this.thingDescriptionTranslations = new HashMap<>();
		this.setNameTranslation(Locale.ENGLISH, defaultThingName);
		this.setDescriptionTranslation(Locale.ENGLISH, defaultThingDescription);
	}

	public Class<?> getThingClass() {
		return this.thingClass;
	}

	public void addChannelTranslation(ChannelTranslation translation) throws TranslationException {
		if (translation != null && translation.getId() != null) {
			if (this.channels.containsKey(translation.getId())) {
				throw new TranslationException("The ChannelTranslation for "+translation.getId()+" is already existing!");
			} else {
				this.channels.put(translation.getId(), translation);
			}
		} else {
			throw new TranslationException("The channeltranslation is null!");
		}
	}
	/**
	 * Get the Channel Translatin by the ChannelId.
	 * @param channelId
	 * @return the Translation object or null.
	 */
	public ChannelTranslation getChannelTranslation(String channelId) {
		return this.channels.get(channelId);
	}

	public void setNameTranslation(Locale locale, String translation) throws TranslationException {
		if (locale != null && translation != null && translation.length() > 0) {
			this.thingNameTranslations.put(locale, translation);
		} else {
			throw new TranslationException("The Locale or translation is Empty!");
		}
	}

	public void setDescriptionTranslation(Locale locale, String translation) throws TranslationException {
		if (locale != null && translation != null && translation.length() > 0) {
			this.thingDescriptionTranslations.put(locale, translation);
		} else {
			throw new TranslationException("The Locale or translation is Empty!");
		}
	}

	public String getName(Locale locale) {
		if(this.thingNameTranslations.containsKey(locale)) {
			return this.thingNameTranslations.get(locale);
		}
		return this.thingNameTranslations.get(Locale.ENGLISH);
	}

	public String getDescription(Locale locale) {
		if(this.thingDescriptionTranslations.containsKey(locale)) {
			return this.thingDescriptionTranslations.get(locale);
		}
		return this.thingDescriptionTranslations.get(Locale.ENGLISH);
	}

	public JsonElement getAsJson(Locale locale) {
		JsonObject translation = new JsonObject();
		translation.addProperty("name", this.thingNameTranslations.get(locale));
		translation.addProperty("description", this.thingDescriptionTranslations.get(locale));
		JsonObject channelTranslations = new JsonObject();
		for(Map.Entry<String, ChannelTranslation> channelTranslation: this.channels.entrySet()) {
			channelTranslations.add(channelTranslation.getKey(), channelTranslation.getValue().getAsJson(locale));
		}
		translation.add("channels", channelTranslations);
		return translation;
	}
}
