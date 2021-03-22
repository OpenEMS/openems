package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.types.OptionsEnum;

public enum SellToGridLimitState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACTIVE_LIMIT(0, "Active limitation"), //
	NO_LIMIT(1, "No active limitation"); //

	private final int value;
	private final String name;

	private SellToGridLimitState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

}
