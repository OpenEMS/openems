package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.types.OptionsEnum;

public enum DelayChargeState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACTIVE_LIMIT(0, "Active limit"), //
	NO_REMAINING_TIME(1, "No remaining time"), //
	NO_REMAINING_CAPACITY(2, "No remaining capacity"), //
	TARGET_MINUTE_NOT_CALCULATED(3, "Target minute not calculated"), //
	NO_FEASABLE_SOLUTION(4, "Limit cannot be adapted because of other constraints with higher priority"), //
	NO_CHARGE_LIMIT(5, "No active limitation"), //
	DISABLED(6, "Delay charge part is disabled"), //
	NOT_STARTED(7, "Delay charge was not started because there is no production or to less production"), //
	AVOID_LOW_CHARGING(8, "Avoid charging with low power for more efficiency"); //

	private final int value;
	private final String name;

	private DelayChargeState(int value, String name) {
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
