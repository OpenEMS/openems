package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class LongSlidingValue extends AbstractNumberSlidingValue<Long> {

	public LongSlidingValue() {
		super(OpenemsType.LONG);
	}

	@Override
	protected Optional<Long> getSlidingValue() {
		var result = this.values.stream() //
				.mapToLong(Long::longValue) //
				.average();
		if (result.isPresent()) {
			var doubleValue = result.getAsDouble();
			return Optional.of(Math.round(doubleValue));
		}
		return Optional.empty();
	}
}
