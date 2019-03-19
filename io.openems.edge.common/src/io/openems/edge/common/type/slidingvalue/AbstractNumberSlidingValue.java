package io.openems.edge.common.type.slidingvalue;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.openems.common.types.OpenemsType;

public abstract class AbstractNumberSlidingValue<T> extends SlidingValue<T> {

	protected final List<T> values = new ArrayList<>();

	protected AbstractNumberSlidingValue(OpenemsType type) {
		super(type);
	}

	@Override
	public synchronized void addValue(T value) {
		if (value != null) {
			this.values.add(value);
		}
	}

	@Override
	protected synchronized void resetValues() {
		this.values.clear();
	}

	@Override
	public String toString() {
		return this.values.stream() //
				.map(Object::toString) //
				.collect(Collectors.joining(","));
	}
}
