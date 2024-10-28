package io.openems.edge.controller.symmetric.thresholdpeakshaver;
import io.openems.common.types.OptionsEnum;

public enum PeakshavingState implements OptionsEnum {
	UNDEFINED(-1, "Undefined State"), //
	IDLE(0, "Idle: Peak Shaver inactive, waiting"), // SoC within operational range
	ERROR(1, "Error State"), //
	DISABLED(2, "Peak Shaver Disabled"), //	
	ACTIVE(3, "Peak Shaving Active"), //
	CHARGING(4, "ESS Charging"), //
	HYSTERESIS_ACTIVE(5, "In Standby: Peak Shaving Paused due to Hysteresis"), //
	CHARGING_FINISHED(6, "Charging Complete: ESS Fully Charged"), //
	DISCHARGING_FAILS(7, "Discharging Failed: ESS Depleted"), //
	PEAKSHAVING_POWER_TOO_LOW(8, "Insufficient ESS Power to Meet Peak Shaving Target"),
	PEAKSHAVING_TARGET_NOT_REACHED(9, "Peak shaving power target differs from real ESS power");


	private final int value;
	private final String name;

	private PeakshavingState(int value, String name) {
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