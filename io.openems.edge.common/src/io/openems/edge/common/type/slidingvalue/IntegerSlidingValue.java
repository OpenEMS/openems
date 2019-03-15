package io.openems.edge.common.type.slidingvalue;

import io.openems.common.types.OpenemsType;

public class IntegerSlidingValue extends AbstractNumberSlidingValue<Integer> {

	@Override
	protected Integer add(Integer a, Integer b) {
		return a + b;
	}

	@Override
	protected Integer divide(Integer a, int b) {
		return a / b;
	}

	protected OpenemsType getType() {
		return OpenemsType.INTEGER;
	}

}
