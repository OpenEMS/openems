package io.openems.edge.sma.enums;

import io.openems.common.types.OptionsEnum;

public enum StatusBatteryApplicationArea implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SELF_CONSUMPTION_RANGE(2614, "Self-Consumption Range"), //
	CONVERSATION_RANGE_OF_SOC(2615, "Conversation Range of State of Charge"), //
	BACKUP_POWER_SUPPLY_RANGE(2616, "Backup Power Supply Range"), //
	DEEP_DISCHARGE_PROTECTION_RANGE(2617, "Deep-Discharge Protection Range"), //
	DEEP_DISCHARGE_RANGE(2618, "Deep-Discharge Range");

	private final int value;
	private final String name;

	private StatusBatteryApplicationArea(int value, String name) {
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