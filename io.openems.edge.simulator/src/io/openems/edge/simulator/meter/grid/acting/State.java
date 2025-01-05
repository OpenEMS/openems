package io.openems.edge.simulator.meter.grid.acting;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1), //
	INITIAL_FREQ(1), //
	FIRST_STEPDOWN_FREQUENCY(2), //
	SECOND_STEPDOWN_FREQUENCY(3), //
	FINISH(4);

	private final int value;

	private State(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	@Override
	public String getName() {
		return this.name();
	}

}
