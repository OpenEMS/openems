package io.openems.edge.kostal.plenticore.enums;

import io.openems.common.types.OptionsEnum;

public enum SensorType implements OptionsEnum {
	SDM_630(0x00, "SDM 630 (B+G E-Tech GmbH)"), //
	B_CONTROL_EM_300_LR(0x01, "B-Control EM-300 LR (TQ Systems)"), //
	RESERVED(0x02, "reserved"), //
	KOSTAL_SMART_ENERGY_METER(0x03, "KOSTAL Smart Energy Meter (KOSTAL)"), //
	NO_SENSOR(0xFF, "No sensor"), //
	UNDEFINED(-1, "Undefined");

	private final int value;
	private final String name;

	private SensorType(int value, String name) {
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
