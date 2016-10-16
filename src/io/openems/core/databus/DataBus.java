package io.openems.core.databus;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;

public class DataBus {
	private final static Logger log = LoggerFactory.getLogger(DataBus.class);
	/**
	 * holds thingId -> channelId -> channel
	 */
	private Map<String, Map<String, Channel>> thingChannels = new HashMap<>();

	/**
	 * holds thingId -> thing
	 */
	private Map<String, Thing> things = new HashMap<>();

	public void addThing(String thingId, Thing thing) {
		HashMap<String, Channel> channels = new HashMap<>();
		// fill channels for this thing
		for (Field field : thing.getClass().getDeclaredFields()) {
			// get all fields of this class
			try {
				Channel channel = (Channel) field.get(thing);
				channels.put(field.getName(), channel);
			} catch (IllegalArgumentException | IllegalAccessException e) {
				// ignore fields that are no Channel
			}
		}
		thingChannels.put(thingId, channels);
	}

	public void channelValueUpdated(Channel channel) {
		try {
			log.info("Channel [" + channel + "] value updated: " + channel.getValue());
		} catch (InvalidValueException e) {
			log.info("Channel [" + channel + "] value updated: INVALID");
		}
	};

	public Set<String> getChannelIds(String thingId) {
		return thingChannels.get(thingId).keySet();
	}

	public Set<String> getThingIds() {
		return things.keySet();
	}
}
