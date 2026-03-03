package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;

public enum PhaseControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	THREE_PHASE_BALANCED_CONTROL(0, "3-phase Balanced Control (balanced output current)"),
	THREE_PHASE_INDIVIDUAL_CONTROL(1, "3-phase Individual Control (unbalanced output current allowed)");

	private final int value;
	private final String name;

	PhaseControlMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override public int getValue() { return this.value; }
	@Override public String getName() { return this.name; }
	@Override public OptionsEnum getUndefined() { return UNDEFINED; }
}
