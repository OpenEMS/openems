package io.openems.common.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.function.ThrowingSupplier;

public class ReflectionUtils {

	public static class ReflectionException extends RuntimeException {
		private static final long serialVersionUID = -8001364348945297741L;

		protected static ReflectionException from(Exception e) {
			return new ReflectionException(e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		public ReflectionException(String message) {
			super(message);
		}
	}

	private ReflectionUtils() {
		// no instance needed
	}

	protected static void callGuarded(ThrowingRunnable<Exception> runnable) throws ReflectionException {
		try {
			runnable.run();
		} catch (Exception e) {
			throw ReflectionException.from(e);
		}
	}

	protected static <T> T callGuarded(ThrowingSupplier<T, Exception> supplier) throws ReflectionException {
		try {
			return supplier.get();
		} catch (Exception e) {
			throw ReflectionException.from(e);
		}
	}

	/**
	 * Sets the value of a Field via Java Reflection.
	 * 
	 * @param object     the target object
	 * @param memberName the name the declared field
	 * @param value      the value to be set
	 * @throws Exception on error
	 */
	public static void setAttributeViaReflection(Object object, String memberName, Object value)
			throws ReflectionException {
		var field = getField(object.getClass(), memberName);
		callGuarded(() -> field.set(object, value));
	}

	/**
	 * Sets the value of a static Field via Java Reflection.
	 * 
	 * @param clazz      the {@link Class}
	 * @param memberName the name the declared field
	 * @param value      the value to be set
	 * @throws Exception on error
	 */
	public static void setStaticAttributeViaReflection(Class<?> clazz, String memberName, Object value)
			throws ReflectionException {
		var field = getField(clazz, memberName);
		callGuarded(() -> field.set(null, value));
	}

	/**
	 * Gets the value of a Field via Java Reflection.
	 * 
	 * @param <T>        the type of the value
	 * @param object     the target object
	 * @param memberName the name the declared field
	 * @return the value
	 * @throws Exception on error
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getValueViaReflection(Object object, String memberName) throws ReflectionException {
		var field = getField(object.getClass(), memberName);
		return (T) callGuarded(() -> field.get(object));
	}

	/**
	 * Invokes a {@link Method} that takes no arguments via Java Reflection.
	 * 
	 * @param <T>        the type of the result
	 * @param object     the target object
	 * @param memberName the name of the method
	 * @return the result of the method
	 * @throws Exception on error
	 */
	public static <T> T invokeMethodWithoutArgumentsViaReflection(Object object, String memberName)
			throws ReflectionException {
		var method = callGuarded(() -> object.getClass().getDeclaredMethod(memberName));
		return invokeMethodViaReflection(object, method);
	}

	/**
	 * Invokes a {@link Method} via Java Reflection.
	 * 
	 * @param <T>    the type of the result
	 * @param object the target object
	 * @param method the {@link Method}
	 * @param args   the arguments to be set
	 * @return the result of the method
	 * @throws Exception on error
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invokeMethodViaReflection(Object object, Method method, Object... args)
			throws ReflectionException {
		method.setAccessible(true);
		return (T) callGuarded(() -> method.invoke(object, args));
	}

	/**
	 * Gets the {@link Class#getDeclaredField(String)} in the given {@link Class} or
	 * any of its superclasses.
	 * 
	 * @param clazz      the given {@link Class}
	 * @param memberName the name of the declared field
	 * @return a {@link Field}
	 * @throws ReflectionException if there is no such field
	 */
	public static Field getField(Class<?> clazz, String memberName) throws ReflectionException {
		try {
			var field = clazz.getDeclaredField(memberName);
			field.setAccessible(true);
			return field;
		} catch (NoSuchFieldException e) {
			// Ignore.
		}
		// If we are here, no matching field or method was found. Search in parent
		// classes.
		Class<?> parent = clazz.getSuperclass();
		if (parent == null) {
			throw new ReflectionException("Reached java.lang.Object");
		}
		return getField(parent, memberName);
	}
}
