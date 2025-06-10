package io.openems.backend.timedata.timescaledb;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public class SimpleDoubleKeyMap<K1, K2, V> implements DoubleKeyMap<K1, K2, V>, Map<K1, Map<K2, V>> {

	private final Map<K1, Map<K2, V>> firstMap;
	private final Function<K1, Map<K2, V>> secondMapSupplier;

	public SimpleDoubleKeyMap(Map<K1, Map<K2, V>> firstImpl, Function<K1, Map<K2, V>> secondMapSupplier) {
		super();
		this.firstMap = Objects.requireNonNull(firstImpl);
		this.secondMapSupplier = Objects.requireNonNull(secondMapSupplier);
	}

	@Override
	public int size() {
		return this.firstMap.size();
	}

	@Override
	public boolean isEmpty() {
		return this.firstMap.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.firstMap.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.firstMap.containsValue(value);
	}

	@Override
	public Map<K2, V> get(Object key) {
		return this.firstMap.get(key);
	}

	@Override
	public V get(K1 key1, K2 key2) {
		var map1 = this.firstMap.get(key1);
		if (map1 == null) {
			return null;
		}
		return map1.get(key2);
	}

	@Override
	public Map<K2, V> put(K1 key, Map<K2, V> value) {
		return this.firstMap.put(key, value);
	}

	@Override
	public V put(K1 key1, K2 key2, V value) {
		return this.firstMap.computeIfAbsent(key1, this.secondMapSupplier).put(key2, value);
	}

	@Override
	public Map<K2, V> remove(Object key) {
		return this.firstMap.remove(key);
	}

	@Override
	public void putAll(Map<? extends K1, ? extends Map<K2, V>> m) {
		this.firstMap.putAll(m);
	}

	@Override
	public void clear() {
		this.firstMap.clear();
	}

	@Override
	public Set<K1> keySet() {
		return this.firstMap.keySet();
	}

	@Override
	public Collection<Map<K2, V>> values() {
		return this.firstMap.values();
	}

	@Override
	public Set<Entry<K1, Map<K2, V>>> entrySet() {
		return this.firstMap.entrySet();
	}

}
