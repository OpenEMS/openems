package io.openems.common.utils;

import java.lang.reflect.InvocationTargetException;

public class ReflectionUtils {

	private ReflectionUtils() {
		// no instance needed
	}

	@SuppressWarnings("unchecked")
	public static <T> boolean setAttribute(Class<? extends T> clazz, T object, String memberName, Object value)
			throws IllegalArgumentException, IllegalAccessException, InvocationTargetException {
		try {
			var field = clazz.getDeclaredField(memberName);
			field.setAccessible(true);
			field.set(object, value);
			return true;
		} catch (NoSuchFieldException e) {
			// Ignore.
		}
		// If we are here, no matching field or method was found. Search in parent
		// classes.
		Class<?> parent = clazz.getSuperclass();
		if (parent == null) {
			return false; // reached 'java.lang.Object'
		}
		return setAttribute((Class<T>) parent, object, memberName, value);
	}

}
