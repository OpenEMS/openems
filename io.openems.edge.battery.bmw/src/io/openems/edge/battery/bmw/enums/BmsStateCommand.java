package io.openems.edge.battery.bmw.enums;

import io.openems.common.types.OptionsEnum;

public enum BmsStateCommand implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	ENABLE_RESERVED_BATTERY(0, "Enable aux power supply"), //
	WAKE_UP(1, "Wake up from stop"), //
	CLOSE_CONTACTOR(2, "Close Contactor"), //
	CLOSE_PRECHARGE(3, "Close Precharge"), //
	CLEAR_ERROR(14, "Clear BMS Error"), //
	RESET_BMS(15, "Reset BMS");

	private final int value;
	private final String name;

	private BmsStateCommand(int value, String name) {
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
