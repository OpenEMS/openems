package io.openems.edge.common.type;

import java.util.TreeMap;

/**
 * Implements a circular buffer with a TreeMap.
 *
 * <p>
 * Be aware that not the eldest entry is removed when the buffer is full, but
 * the entry with the lowest key is removed!
 *
 * @param <K> the type of the Key
 * @param <V> the type of the Value
 */
public class CircularTreeMap<K, V> extends TreeMap<K, V> {

	private static final long serialVersionUID = 1L;

	private final int limit;

	public CircularTreeMap(int limit) {
		this.limit = limit;
	}

	@Override
	public V put(K key, V value) {
		var result = super.put(key, value);
		if (super.size() > this.limit) {
			this.removeLowest();
		}
		return result;
	}

	private void removeLowest() {
		var iterator = this.keySet().iterator();
		if (iterator.hasNext()) {
			this.remove(iterator.next());
		}
	}

}
