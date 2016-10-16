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

public class DataBusFactory {
	private static Logger log = LoggerFactory.getLogger(DataBusFactory.class);

	public static Map<String, DataChannel> getDataChannels(Thing thing, DataBus dataBus) {
		HashMap<String, DataChannel> dataChannels = new HashMap<>();
		// fill channels for this thing
		for (Method method : thing.getClass().getDeclaredMethods()) {
			// get all methods of this class
			if (method.getReturnType().isAssignableFrom(Channel.class)) {
				// method returns a Channel; now check for the annotation
				IsChannel annotation = getAnnotatedMethod(thing.getClass(), method.getName());
				if (annotation != null) {
					try {
						Channel channel = (Channel) method.invoke(thing);
						channel.setDataBus(dataBus);
						DataChannel dataChannel = new DataChannel(channel, annotation.id(), annotation.address());
						dataChannels.put(annotation.id(), dataChannel);
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.warn("Unable to add Channel to DataBus. Method [" + method.getName() + "], ChannelId ["
								+ annotation.id() + "], Address [" + annotation.address() + "]: " + e.getMessage());
					}
				}
			}
		}
		return dataChannels;
	}

	/**
	 * Searches the class tree for a method with the name methodName, that is annotated with IsChannel.
	 *
	 * @param clazz
	 * @param methodName
	 * @return IsChannel annotation or null if no match was found
	 */
	private static IsChannel getAnnotatedMethod(Class<?> clazz, String methodName) {
		// clazz must be a Thing
		if (!Thing.class.isInterface() && !Thing.class.isAssignableFrom(clazz)) {
			return null;
		}
		// check if method can be found
		Method method;
		try {
			method = clazz.getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			return null;
		}
		// return the annotation if found
		IsChannel annotation = method.getAnnotation(IsChannel.class);
		if (annotation != null) {
			return annotation;
		}
		// start recursive search if not found
		for (Class<?> implementedInterface : clazz.getInterfaces()) {
			// search all implemented interfaces
			IsChannel ret = getAnnotatedMethod(implementedInterface, methodName);
			if (ret != null) {
				return ret;
			}
		}
		if (clazz.getSuperclass() == null) {
			// reached the top end... no superclass found
			return null;
		}
		return getAnnotatedMethod(clazz.getSuperclass(), methodName);
	}
}
