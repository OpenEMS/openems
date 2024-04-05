package io.openems.edge.common.type.slidingvalue;

import java.util.Optional;

import io.openems.common.types.OpenemsType;

public class DoubleSlidingValue extends AbstractNumberSlidingValue<Double> {

	public DoubleSlidingValue() {
		super(OpenemsType.DOUBLE);
	}

	@Override
	protected Optional<Double> getSlidingValue() {
		var result = this.values.stream() //
				.mapToDouble(Double::doubleValue) //
				.average();
		if (result.isPresent()) {
			return Optional.of(result.getAsDouble());
		}
		return Optional.empty();
	}

}
