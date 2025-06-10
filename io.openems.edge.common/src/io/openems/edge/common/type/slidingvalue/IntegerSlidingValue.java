package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class IntegerSlidingValue extends AbstractNumberSlidingValue<Integer> {

	public IntegerSlidingValue() {
		super(OpenemsType.INTEGER);
	}

	@Override
	protected Optional<Integer> getSlidingValue() {
		var result = this.values.stream() //
				.mapToInt(Integer::intValue) //
				.average();
		if (!result.isPresent()) {
			return Optional.empty();
		}
		var doubleValue = result.getAsDouble();
		var value = Math.round(doubleValue);
		if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
			return Optional.empty();
		} else {
			return Optional.of((int) value);
		}
	}
}
