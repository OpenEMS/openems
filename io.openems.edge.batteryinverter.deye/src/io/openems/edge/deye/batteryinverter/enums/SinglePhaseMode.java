package io.openems.edge.deye.batteryinverter.enums;

import io.openems.common.types.OptionsEnum;

public enum SinglePhaseMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	DISABLE(0, "Disable"), //
	SINGLE_PHASE_230V(1, "Single Phae 230V"), //
	SINGLE_PHASE_480V(2, "Single Phase 480V");//

	private final int value;
	private final String name;

	private SinglePhaseMode(int value, String name) {
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