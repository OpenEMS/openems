package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class ShortSlidingValue extends AbstractNumberSlidingValue<Short> {

	public ShortSlidingValue() {
		super(OpenemsType.SHORT);
	}

	@Override
	protected Optional<Short> getSlidingValue() {
		var result = this.values.stream() //
				.mapToInt(Short::intValue) //
				.average();
		if (!result.isPresent()) {
			return Optional.empty();
		}
		var doubleValue = result.getAsDouble();
		var value = Math.round(doubleValue);
		if (value < Short.MIN_VALUE || value > Short.MAX_VALUE) {
			return Optional.empty();
		} else {
			return Optional.of((short) value);
		}
	}
}
