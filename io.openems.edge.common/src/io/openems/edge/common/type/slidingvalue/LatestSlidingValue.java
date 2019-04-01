package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class LatestSlidingValue extends SlidingValue<Object> {

	private Object value = null;

	public LatestSlidingValue(OpenemsType type) {
		super(type);
	}

	@Override
	public synchronized void addValue(Object value) {
		this.value = value;
	}

	@Override
	protected Optional<Object> getSlidingValue() {
		return Optional.ofNullable(this.value);
	}

	@Override
	protected void resetValues() {
		this.value = null;
	}

	@Override
	public String toString() {
		return String.valueOf(this.value);
	}
}
