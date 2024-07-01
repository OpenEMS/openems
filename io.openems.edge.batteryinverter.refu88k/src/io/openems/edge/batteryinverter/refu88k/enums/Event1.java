package io.openems.edge.batteryinverter.refu88k.enums;

import io.openems.common.types.OptionsEnum;

public enum Event1 implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	GROUND_FAULT(0, "Ground fault"), //
	DC_OVER_VOLTAGE(1, "Dc over voltage"), //
	AC_DISCONNECT(2, "AC disconnect open"), //
	DC_DISCONNECT(3, "DC disconnect open"), //
	GRID_DISCONNECT(4, "Grid shutdown"), //
	CABINET_OPEN(5, "Cabinet open"), //
	MANUAL_SHUTDOWN(6, "Manual shutdown"), //
	OVER_TEMP(7, "Over temperature"), //
	OVER_FREQUENCY(8, "Frequency above limit"), //
	UNDER_FREQUENCY(9, "Frequency under limit"), //
	AC_OVER_VOLT(10, "AC Voltage above limit"), //
	AC_UNDER_VOLT(11, "AC Voltage under limit"), //
	BLOWN_STRING_FUSE(12, "Blown String fuse on input"), //
	UNDER_TEMP(13, "Under temperature"), //
	MEMORY_LOSS(14, "Generic Memory or Communication error (internal)"), //
	HW_TEST_FAILURE(15, "Hardware test failure"), //
	/**
	 * When OTHER_ALARM is set, it indicates that some other alarm has occurred in
	 * the PCS (i.e. an alarm which does not map to one of the other alarm
	 * categories).
	 */
	OTHER_ALARM(16, "Other alarm"), //
	/**
	 * When OTHER_WARNING is set, it indicates that some other warning has occurred
	 * in the PCS (i.e. a warning which does not map to one of the other warning
	 * categories).
	 */
	OTHER_WARNING(17, "Other warning");

	private final int value;
	private final String name;

	private Event1(int value, String name) {
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
