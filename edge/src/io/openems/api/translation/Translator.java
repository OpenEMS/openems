package io.openems.api.translation;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonObject;

public class Translator {

	private static Translator instance;
	private Map<Class<?>, ThingTranslation> thingTranslations;

	private Translator() {
		this.thingTranslations = new HashMap<>();
	}

	public static Translator getInstance() {
		if(instance == null) {
			instance = new Translator();
		}
		return instance;
	}

	public void addThingTranslation(ThingTranslation translation) throws TranslationException {
		if (translation != null && translation.getThingClass() != null) {
			if (this.thingTranslations.containsKey(translation.getThingClass())) {
				throw new TranslationException("The ThingTranslation for "+translation.getThingClass()+" is already existing!");
			} else {
				this.thingTranslations.put(translation.getThingClass(), translation);
			}
		} else {
			throw new TranslationException("The thingtranslation is null!");
		}
	}

	public ThingTranslation getThingTranslation(Class<?> thingClass) {
		return this.thingTranslations.get(thingClass);
	}

	public JsonObject getAsJson(Locale locale) {
		JsonObject translation = new JsonObject();
		for(Map.Entry<Class<?>, ThingTranslation> thingTranslation:thingTranslations.entrySet()) {
			translation.add(thingTranslation.getKey().getName(), thingTranslation.getValue().getAsJson(locale));
		}
		return translation;
	}

}
