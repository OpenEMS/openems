package io.openems.core.databus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;

public class DatabusFactory {
	private static Logger log = LoggerFactory.getLogger(DatabusFactory.class);

	public static Map<String, DataChannelMapping> getDataChannels(Thing thing, Databus databus) {
		HashMap<String, DataChannelMapping> dataChannels = new HashMap<>();
		// fill channels for this thing
		for (Method method : thing.getClass().getDeclaredMethods()) {
			// get all methods of this class
			if (method.getReturnType().isAssignableFrom(Channel.class)) {
				// method returns a Channel; now check for the annotation
				IsChannel annotation = InjectionUtils.getIsChannelMethods(thing.getClass(), method.getName());
				if (annotation != null) {
					try {
						Channel channel = (Channel) method.invoke(thing);
						channel.setDatabus(databus);
						DataChannelMapping dataChannel = new DataChannelMapping(channel, annotation.id(),
								annotation.address());
						dataChannels.put(annotation.id(), dataChannel);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.warn("Unable to add Channel to Databus. Method [" + method.getName() + "], ChannelId ["
								+ annotation.id() + "], Address [" + annotation.address() + "]: " + e.getMessage());
					}
				}
			}
		}
		return dataChannels;
	}

}
