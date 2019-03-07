package io.openems.edge.controller.api.backend.slidingvalue;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNumberSlidingValue<T> extends SlidingValue<T> {

	private final List<T> values = new ArrayList<>();

	/**
	 * Add two numbers.
	 * 
	 * @param a first number; guaranteed to be not-null
	 * @param b second number; guaranteed to be not-null
	 * @return the sum
	 */
	protected abstract T add(T a, T b);

	public synchronized void addValue(T value) {
		this.values.add(value);
	}

	@Override
	protected synchronized T getSlidingValue() {
		T result = null;
		for (T value : this.values) {
			if (value == null) {
				// nothing
			} else if (result == null) {
				result = value;
			} else {
				value = this.add(value, result);
			}
		}
		return result;
	}

	@Override
	protected synchronized void resetValues() {
		this.values.clear();
	}

}
