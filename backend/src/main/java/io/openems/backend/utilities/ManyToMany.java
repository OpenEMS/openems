package io.openems.backend.utilities;

// Source: http://stackoverflow.com/questions/20390923/do-we-have-a-multibimap
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class ManyToMany<K, V> {
	private final SetMultimap<K, V> keysToValues = HashMultimap.create();

	private final SetMultimap<V, K> valuesToKeys = HashMultimap.create();

	public Set<V> getValues(K key) {
		return keysToValues.get(key);
	}

	public Set<K> getKeys(V value) {
		return valuesToKeys.get(value);
	}

	public boolean put(K key, V value) {
		return keysToValues.put(key, value) && valuesToKeys.put(value, key);
	}

	public boolean putAll(K key, Iterable<? extends V> values) {
		boolean changed = false;
		for (V value : values) {
			changed = put(key, value) || changed;
		}
		return changed;
	}

	public boolean remove(K key, V value) {
		return keysToValues.remove(key, value) && valuesToKeys.remove(value, key);
	}

	public void removeAllKeys(K key) {
		keysToValues.get(key).forEach(value -> {
			valuesToKeys.removeAll(value);
		});
		keysToValues.removeAll(key);
	}

	public void removeAllValues(V value) {
		valuesToKeys.get(value).forEach(key -> {
			keysToValues.removeAll(key);
		});
		valuesToKeys.removeAll(value);
	}
}
