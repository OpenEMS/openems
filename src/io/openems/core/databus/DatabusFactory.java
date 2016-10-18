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

	public static Map<String, DataChannel> getDataChannels(Thing thing, Databus databus) {
		HashMap<String, DataChannel> dataChannels = new HashMap<>();
		// fill channels for this thing
		for (Method method : thing.getClass().getDeclaredMethods()) {
			// get all methods of this class
			if (Channel.class.isAssignableFrom(method.getReturnType())) {
				// method returns a Channel; now check for the annotation
				IsChannel annotation = InjectionUtils.getIsChannelMethods(thing.getClass(), method.getName());
				if (annotation != null) {
					try {
						Channel channel = (Channel) method.invoke(thing);
						channel.setDatabus(databus);
						DataChannel dataChannel = new DataChannel(thing, thing.getThingId(), channel, annotation.id());
						dataChannels.put(annotation.id(), dataChannel);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.warn("Unable to add Channel to Databus. Method [" + method.getName() + "], ChannelId ["
								+ annotation.id() + "]: " + e.getMessage());
					}
				}
			}
		}
		return dataChannels;
	}

}
