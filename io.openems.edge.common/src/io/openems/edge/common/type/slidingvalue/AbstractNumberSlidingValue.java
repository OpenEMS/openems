package io.openems.edge.common.type.slidingvalue;

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

	/**
	 * Divide first number by second number.
	 * 
	 * @param a first number; guaranteed to be not-null
	 * @param b second number; guaranteed to be > 0
	 * @return the sum
	 */
	protected abstract T divide(T a, int b);

	public synchronized void addValue(T value) {
		this.values.add(value);
	}

	@Override
	protected synchronized T getSlidingValue() {
		T result = null;
		int noOfNotNullValues = 0;
		for (T value : this.values) {
			if (value == null) {
				// nothing
			} else {
				noOfNotNullValues++;
				if (result == null) {
					result = value;
				} else {
					value = this.add(value, result);
				}
			}
		}
		if (noOfNotNullValues > 0 && result != null) {
			return this.divide(result, noOfNotNullValues);
		} else {
			return result;
		}
	}

	@Override
	protected synchronized void resetValues() {
		this.values.clear();
	}

}
