package io.openems.edge.controller.api.backend.slidingvalue;

import io.openems.common.types.OpenemsType;

public class IntegerSlidingValue extends AbstractNumberSlidingValue<Integer> {

	@Override
	protected Integer add(Integer a, Integer b) {
		return a + b;
	}

	protected OpenemsType getType() {
		return OpenemsType.INTEGER;
	}

}
