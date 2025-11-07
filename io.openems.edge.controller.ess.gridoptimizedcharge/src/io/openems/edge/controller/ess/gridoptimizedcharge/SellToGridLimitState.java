package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.types.OptionsEnum;

public enum SellToGridLimitState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACTIVE_LIMIT_FIXED(0, "Active limitation - Fix limit"), //
	NO_LIMIT(1, "No active limitation"), //
	NO_FEASABLE_SOLUTION(2, "Limit cannot be adapted because of other constraints with higher priority"), //
	ACTIVE_LIMIT_CONSTRAINT(3, "Active limitation - Minimum charge power "), //
	DISABLED(4, "SellToGridLimit part is disabled"), //
	NOT_STARTED(5, "SellToGridLimit part was not started because there is no production or to less production"); //

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
