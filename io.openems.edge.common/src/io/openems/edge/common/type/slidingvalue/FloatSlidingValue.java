package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class FloatSlidingValue extends AbstractNumberSlidingValue<Float> {

	public FloatSlidingValue() {
		super(OpenemsType.FLOAT);
	}

	@Override
	protected Optional<Float> getSlidingValue() {
		var result = this.values.stream() //
				.mapToDouble(Float::doubleValue) //
				.average();
		if (!result.isPresent()) {
			return Optional.empty();
		}
		var value = result.getAsDouble();
		if (value < Float.MIN_VALUE || value > Float.MAX_VALUE) {
			return Optional.empty();
		} else {
			return Optional.of((float) value);
		}
	}

}
