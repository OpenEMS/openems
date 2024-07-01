package io.openems.edge.core.componentmanager;

import java.util.Dictionary;
import java.util.Optional;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Provides helper utilities to handle {@link Dictionary}s.
 */
public class DictionaryUtils {

	/**
	 * Get the Dictionary value as String.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link String}
	 */
	public static String getAsString(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.STRING, dict.get(key));
	}

	/**
	 * Get the Dictionary value as Optional String.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link String}
	 */
	public static Optional<String> getAsOptionalString(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsString(dict, key));
	}

	/**
	 * Get the Dictionary value as Integer.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Integer}
	 */
	public static Integer getAsInteger(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.INTEGER, dict.get(key));
	}

	/**
	 * Get the Dictionary value as Optional Integer.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link Integer}
	 */
	public static Optional<Integer> getAsOptionalInteger(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsInteger(dict, key));
	}

	/**
	 * Get the Dictionary value as Boolean.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Integer}
	 */
	public static Boolean getAsBoolean(Dictionary<String, Object> dict, String key) {
		return TypeUtils.getAsType(OpenemsType.BOOLEAN, dict.get(key));
	}

	/**
	 * Get the Dictionary value as Optional Boolean.
	 *
	 * @param dict the {@link Dictionary}
	 * @param key  the identifier key
	 * @return the value as {@link Optional} {@link Integer}
	 */
	public static Optional<Boolean> getAsOptionalBoolean(Dictionary<String, Object> dict, String key) {
		return Optional.ofNullable(getAsBoolean(dict, key));
	}

}
