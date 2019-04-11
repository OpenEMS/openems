package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;
import java.util.OptionalDouble;

import io.openems.common.types.OpenemsType;

public class LongSlidingValue extends AbstractNumberSlidingValue<Long> {

	public LongSlidingValue() {
		super(OpenemsType.LONG);
	}

	@Override
	protected Optional<Long> getSlidingValue() {
		OptionalDouble result = this.values.stream() //
				.mapToLong(Long::longValue) //
				.average();
		if (result.isPresent()) {
			double doubleValue = result.getAsDouble();
			return Optional.of(Math.round(doubleValue));
		} else {
			return Optional.empty();
		}
	}
}
