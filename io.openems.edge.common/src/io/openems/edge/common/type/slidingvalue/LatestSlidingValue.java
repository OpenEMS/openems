package io.openems.edge.common.type.slidingvalue;

import io.openems.common.types.OpenemsType;

public class LatestSlidingValue extends SlidingValue<Object> {

	private Object value = null;

	private OpenemsType type;

	public LatestSlidingValue(OpenemsType type) {
		this.type = type;
	}

	@Override
	public synchronized void addValue(Object value) {
		this.value = value;
	}

	@Override
	protected synchronized Object getSlidingValue() {
		return this.value;
	}

	@Override
	protected void resetValues() {
		this.value = null;
	}

	@Override
	protected OpenemsType getType() {
		return this.type;
	}

}
