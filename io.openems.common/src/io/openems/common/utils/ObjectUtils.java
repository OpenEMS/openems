package io.openems.common.utils;

import java.util.Optional;

public class ObjectUtils {

	/**
	 * Cast and return the given {@link Object} as a {@link String}. If given object
	 * is no {@link String} it will return null.
	 *
	 * @param object to cast an return as {@link String}
	 * @return a {@link String} or null
	 */
	public static String getAsString(Object object) {
		return switch (object) {
		case String s -> s;
		case null, default -> null;
		};
	}

	/**
	 * Gets the given {@link Object} as a {@link Optional} {@link String}.
	 *
	 * @param object to get as {@link Optional}
	 * @return the {@link Optional} {@link String} value
	 */
	public static Optional<String> getAsOptionalString(Object object) {
		return Optional.ofNullable(ObjectUtils.getAsString(object));
	}

	/**
	 * Cast and return the given {@link Object} as a {@link Integer}. If given
	 * object is no {@link Integer} it will return null.
	 *
	 * @param object to cast an return as {@link Integer}
	 * @return a {@link Integer} or null
	 */
	public static Integer getAsInteger(Object object) {
		return switch (object) {
		case Integer i -> i;
		case null, default -> null;
		};
	}

	/**
	 * Cast and return the given {@link Object} as a {@link Object} array. If given
	 * object is no {@link Object} array it will return an empty array.
	 *
	 * @param object to cast an return as {@link Object} array
	 * @return a {@link Object} array or empty array
	 */
	public static Object[] getAsObjectArrray(Object object) {
		return switch (object) {
		case Object[] os -> os;
		case null, default -> null;
		};
	}
}
