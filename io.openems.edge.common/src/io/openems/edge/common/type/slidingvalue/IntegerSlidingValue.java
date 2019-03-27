package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;
import java.util.OptionalDouble;

import io.openems.common.types.OpenemsType;

public class IntegerSlidingValue extends AbstractNumberSlidingValue<Integer> {

	public IntegerSlidingValue() {
		super(OpenemsType.INTEGER);
	}

	@Override
	protected Optional<Integer> getSlidingValue() {
		OptionalDouble result = this.values.stream() //
				.mapToInt(Integer::intValue) //
				.average();
		if (result.isPresent()) {
			double doubleValue = result.getAsDouble();
			long value = Math.round(doubleValue);
			if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
				return Optional.empty();
			} else {
				return Optional.of((int) value);
			}
		} else {
			return Optional.empty();
		}
	}
}
