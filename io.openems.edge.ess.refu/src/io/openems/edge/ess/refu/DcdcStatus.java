package io.openems.edge.ess.refu;

import io.openems.common.types.OptionsEnum;

public enum DcdcStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	READY_TO_POWER_ON(1, "Ready to Power on"), //
	READY_FOR_OPERATING(2, "Ready for Operating"), //
	ENABLED(4, "Enabled"), //
	DCDC_FAULT(8, "DCDC Fault"), //
	DCDC_WARNING(128, "DCDC Warning"), //
	VOLTAGE_CURRENT_MODE(256, "Voltage/Current mode"), //
	POWER_MODE(512, "Power mode");

	private final int value;
	private final String option;

	private DcdcStatus(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return option;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}