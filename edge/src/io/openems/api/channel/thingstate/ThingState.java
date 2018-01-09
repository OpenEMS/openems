package io.openems.api.channel.thingstate;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import io.openems.api.channel.ChannelEnum;

public enum ThingState implements ChannelEnum {
	RUN(0), WARNING(1), FAULT(2);

	private static Map<Locale, ResourceBundle> resources = new HashMap<>();

	private int value;

	private ThingState(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName(Locale locale) {
		ResourceBundle resource = getResource(locale);
		if(resource != null) {
			try {
				return resource.getString(name());
			}catch(MissingResourceException e) {
				// no handling needed
			}
		}
		return name();
	}

	private static ResourceBundle getResource(Locale locale) {
		ResourceBundle resource = resources.get(locale);
		if (resource != null) {
			return resource;
		} else {
			try {
				resource = ResourceBundle.getBundle("ThingStateNames", locale);
				resources.put(locale, resource);
				return resource;
			} catch (MissingResourceException e) {
				try {
					resource = ResourceBundle.getBundle("ThingStateNames");
					resources.put(locale, resource);
					return resource;
				} catch (MissingResourceException ex) {
					return null;
				}
			}
		}
	}
}
