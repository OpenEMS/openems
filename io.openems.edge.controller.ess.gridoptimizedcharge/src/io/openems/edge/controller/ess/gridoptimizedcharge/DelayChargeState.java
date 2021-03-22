package io.openems.edge.controller.ess.gridoptimizedcharge;

import io.openems.common.types.OptionsEnum;

public enum DelayChargeState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ACTIVE_LIMIT(0, "Active limit"), //
	PASSED_TARGET_HOUR(1, "Passed target hour"), //
	NO_REMAINING_CAPACITY(2, "No remaining capacity"), //
	TARGET_HOUR_NOT_CALCULATED(3, "target hour not calculated"), //
	NO_FEASABLE_SOLUTION(4, "Limit cannot be adapted because of other constraints with higher priority"); //

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
