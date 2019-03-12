package io.openems.edge.common.type.slidingvalue;

import io.openems.common.types.OpenemsType;

public class LongSlidingValue extends AbstractNumberSlidingValue<Long> {

	@Override
	protected Long add(Long a, Long b) {
		return a + b;
	}

	protected OpenemsType getType() {
		return OpenemsType.LONG;
	}

}
