package io.openems.backend.timedata.timescaledb;

import java.util.Map;

public interface DoubleKeyMap<K1, K2, V> extends Map<K1, Map<K2, V>> {

	/**
	 * Gets the value in the map associated with the keys.
	 * 
	 * @param key1 the key of the first map
	 * @param key2 the key of the second map
	 * @return the value or null if not found
	 */
	public V get(K1 key1, K2 key2);

	/**
	 * Adds a value to the map.
	 * 
	 * @param key1  the first key
	 * @param key2  the second key
	 * @param value the value to put
	 * @return the value
	 */
	public V put(K1 key1, K2 key2, V value);

}
