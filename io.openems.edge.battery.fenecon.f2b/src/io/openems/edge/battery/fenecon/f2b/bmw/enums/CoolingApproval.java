package io.openems.edge.battery.fenecon.f2b.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum CoolingApproval implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_GRANTED(0, "Cooling approval is not granted"),//
	GRANTED(1, "Cooling approval is granted"),//
	SYSTEM_ERROR(2, "System error"),//
	;//

	private final int value;
	private final String name;

	private CoolingApproval(int value, String name) {
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