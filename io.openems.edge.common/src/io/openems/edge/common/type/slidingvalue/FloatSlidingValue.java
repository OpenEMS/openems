package io.openems.edge.common.type.slidingvalue;

import io.openems.common.types.OpenemsType;

public class FloatSlidingValue extends AbstractNumberSlidingValue<Float> {

	@Override
	protected Float add(Float a, Float b) {
		return a + b;
	}

	protected OpenemsType getType() {
		return OpenemsType.FLOAT;
	}

}
