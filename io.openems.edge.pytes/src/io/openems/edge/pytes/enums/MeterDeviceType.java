package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;
// 
public enum MeterDeviceType implements OptionsEnum {
    UNDEFINED(-1, "Undefined"),
    INTERNAL(0, "Internal with CTs connected"),
    EXTERNAL(1, "External Power Meter (EPM) connected via Modbus");
	
	private final int value;
	private final String name;

	private MeterDeviceType(int value, String name) {
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
