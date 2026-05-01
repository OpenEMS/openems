package io.openems.edge.core.componentmanager;

import java.util.Map;
import java.util.Optional;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.type.TypeUtils;

/**
 * Provides helper utilities to handle {@link Map}s.
 */
class MapUtils {

	private MapUtils() {
		// ignored
	}

	/**
	 * Get the Map value as Optional Object. Returns an empty Optional if the map is
	 * null or if the key is not present in the map.
	 * 
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link Object}
	 */
	public static Optional<Object> getAsOptional(Map<String, Object> map, String key) {
		return map == null ? Optional.empty() : Optional.ofNullable(map.get(key));
	}

	/**
	 * Get the Map value as Optional String. Returns an empty Optional if the map is
	 * null or if the key is not present in the map.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link String}
	 */
	public static Optional<String> getAsOptionalString(Map<String, Object> map, String key) {
		return getAsOptional(map, key).map(v -> TypeUtils.getAsType(OpenemsType.STRING, v));
	}

	/**
	 * Get the Map value as Optional Boolean. Returns an empty Optional if the map
	 * is null or if the key is not present in the map.
	 *
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link Boolean}
	 */
	public static Optional<Boolean> getAsOptionalBoolean(Map<String, Object> map, String key) {
		return getAsOptional(map, key).map(v -> TypeUtils.getAsType(OpenemsType.BOOLEAN, v));
	}

	/**
	 * Get the Map value as Optional Long. Returns an empty Optional if the map is
	 * null or if the key is not present in the map.
	 * 
	 * @param map the {@link Map}
	 * @param key the identifier key
	 * @return the value as {@link Optional} {@link Long}
	 */
	public static Optional<Long> getAsOptionalLong(Map<String, Object> map, String key) {
		return getAsOptional(map, key).map(v -> TypeUtils.getAsType(OpenemsType.LONG, v));
	}

}
