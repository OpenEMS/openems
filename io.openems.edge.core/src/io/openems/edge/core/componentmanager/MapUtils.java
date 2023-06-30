package io.openems.edge.core.componentmanager;

import java.util.Map;
import java.util.Optional;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Provides helper utilities to handle {@link Map}s.
 */
public class MapUtils {

	/**
	 * Get the Map value as String.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link String}
	 */
	public static String getAsString(Map<String, Object> map, String key) {
		return TypeUtils.getAsType(OpenemsType.STRING, map.get(key));
	}

	/**
	 * Get the Map value as Optional String.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link String}
	 */
	public static Optional<String> getAsOptionalString(Map<String, Object> map, String key) {
		return Optional.ofNullable(getAsString(map, key));
	}

	/**
	 * Get the Map value as Boolean.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Integer}
	 */
	public static Boolean getAsBoolean(Map<String, Object> map, String key) {
		return TypeUtils.getAsType(OpenemsType.BOOLEAN, map.get(key));
	}

	/**
	 * Get the Map value as Optional Boolean.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link Integer}
	 */
	public static Optional<Boolean> getAsOptionalBoolean(Map<String, Object> map, String key) {
		return Optional.ofNullable(getAsBoolean(map, key));
	}

}
