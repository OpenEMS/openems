package io.openems.edge.common.type;

import java.util.Iterator;
import java.util.TreeMap;

public class CircularTreeMap<K, V> extends TreeMap<K, V> {

	private static final long serialVersionUID = 1L;

	private final int limit;

	public CircularTreeMap(int limit) {
		this.limit = limit;
	}

	@Override
	public V put(K key, V value) {
		V result = super.put(key, value);
		if (super.size() > this.limit) {
			this.removeEldest();
		}
		return result;
	}

	private void removeEldest() {
		Iterator<K> iterator = this.keySet().iterator();
		if (iterator.hasNext()) {
			this.remove(iterator.next());
		}
	}

}
