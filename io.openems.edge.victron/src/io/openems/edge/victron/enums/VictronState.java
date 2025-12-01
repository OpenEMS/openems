package io.openems.edge.victron.enums;

import io.openems.common.types.OptionsEnum;

public enum VictronState implements OptionsEnum {
	UNDEFINED(-1, "undefined"), //
	INITIALIZING_WAIT_START(0, "Initializing (Wait start)"), //
	INITIALIZING_BEFORE_BOOT(1, "Initialiying (before boot)"), //
	INITIALIZING_BEFORE_BOOT_DELAY(2, "Initialiying (before boot delay)"), //
	INITIALIZING_WAIT_BOOT(3, "Initialiying (Wait boot)"), //
	INITIALIZING(4, "Initializing"), //
	INITIALIZING_MEASURE_BATTERY_VOLTAGE(5, "Initializing (Measure battery voltage)"), //
	INITIALIZING_CALCULATE_BATTERY_VOLTAGE(6, "Initializing (Calculate battery voltage)"), //
	INITIALIZING_WAIT_BUS_VOLTAGE(7, "Initializing (Wait bus voltage)"), //
	INITIALIZING_WAIT_FOR_LYNX_SHUNT(8, "Initializing (Wait for lynx shunt)"), //
	RUNNING(9, "Running"), //
	ERROR(10, "Error"), //
	UNUSED(11, "unused"), //
	SHUTDOWN(12, "Shutdown"), //
	SLAVE_UPDATING(13, "Slave updating"), //
	STANDBY(14, "Standby"), //
	GOING_TO_RUN(15, "Going to run"), //
	PRE_CHARGING(16, "Pre-charging");

	private final int value;
	private final String option;

	private VictronState(int value, String option) {
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
