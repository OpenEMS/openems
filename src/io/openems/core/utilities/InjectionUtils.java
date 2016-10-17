package io.openems.core.utilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.openems.api.channel.IsChannel;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;

public class InjectionUtils {

	/**
	 * Creates an instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * Restriction: this implementation tries only the first constructor of the Class.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws ConfigException
	 */
	public static Object getInstance(Class<?> clazz, Object... args) throws ConfigException {
		try {
			if (args.length == 0) {
				return clazz.newInstance();
			} else {
				Constructor<?> constructor = clazz.getConstructors()[0];
				return constructor.newInstance(args);
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			throw new ConfigException("Unable to instantiate class [" + clazz.getName() + "]: " + e.getMessage());
		}
	}

	/**
	 * Searches the class tree for a method with the name methodName, that is annotated with IsChannel.
	 *
	 * @param clazz
	 * @param methodName
	 * @return IsChannel annotation or null if no match was found
	 */
	public static IsChannel getIsChannelMethods(Class<?> clazz, String methodName) {
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
			IsChannel ret = getIsChannelMethods(implementedInterface, methodName);
			if (ret != null) {
				return ret;
			}
		}
		if (clazz.getSuperclass() == null) {
			// reached the top end... no superclass found
			return null;
		}
		return getIsChannelMethods(clazz.getSuperclass(), methodName);
	}
}
