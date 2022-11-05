package io.openems.edge.kaco.blueplanet.hybrid10;

import io.openems.common.types.OptionsEnum;

public enum BatteryStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ERROR(0, "Error"), //
	OFF_STANDBY(1, "Off/Standby"), //
	TEST_2(2, "Test 2"), //
	TEST_3(3, "Test 3"), //
	TEST_4(4, "Test 4"), //
	TEST_5(5, "Test 5"), //
	TEST_6(6, "Test 6"), //
	TEST_7(7, "Test 7"), //
	TEST_8(8, "Test 8"), //
	TEST_9(9, "Test 9"), //
	TEST_10(10, "Test 10"), //
	TEST_11(11, "Test 11"), //
	TEST_12(12, "Test 12"), //
	TEST_13(13, "Test 13"), //
	TEST_14(14, "Test 14"), //
	TEST_15(15, "Test 15"), //
	TEST_16(16, "Test 16"), //
	ON_ACTIVE(17, "On/Active"), //
	POWER_DOWN(18, "Power down"), //
	SOFTWARE_UPDATE(19, "Software Update");

	private final int value;
	private final String name;

	private BatteryStatus(int value, String name) {
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