package io.openems.edge.goodwe.common.enums;

import io.openems.common.types.OptionsEnum;

public enum WaveformDetection implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	HIGH_PRECISION(0, "Off-Grid operation is prioritized with high precision"), //
	LOW_PRECISION(1, "Off-Grid operation is prioritized with low precision"), //
	DETECTION_DISABLED(2, "VDE 4110 Ride Through operation is prioritized") //
	;

	private final int value;
	private final String option;

	private WaveformDetection(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}