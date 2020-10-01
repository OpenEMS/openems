package io.openems.edge.controller.ess.delayedselltogrid;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ABOVE_SELL_TO_GRID_LIMIT(0, "Above Sell To Grid Limit"), //
	BETWEEN_CONTINUOUS_SELL_TO_GRID_AND_SELL_TO_GRID_LIMIT(0, "Between Continuous Sell To Grid And Sell To Grid Limit"), //
	UNDER_CONTINUOUS_SELL_TO_GRID(3, "Under Continuous Sell To Grid"),//
	;

	private final int value;
	private final String name;

	private State(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}