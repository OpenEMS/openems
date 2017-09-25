package io.openems.backend.utilities;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;

public class MultiKeyMap<K1, K2, V> {
	private Map<K1, V> k1Map = new ConcurrentHashMap<>();
	private BiMap<K2, K1> k2Map = Maps.synchronizedBiMap(HashBiMap.create());

	public MultiKeyMap() {}

	public void put(K1 key1, K2 key2, V value) {
		k1Map.put(key1, value);
		k2Map.forcePut(key2, key1);
	}

	public void put(K1 key1, V value) {
		k1Map.put(key1, value);
		k2Map.inverse().remove(key1);
	}

	public V getWithKey1(K1 key1) {
		return k1Map.get(key1);
	}

	public V getWithKey2(K2 key2) {
		K1 key1 = k2Map.get(key2);
		return getWithKey1(key1);
	}
}
