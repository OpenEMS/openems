package io.openems.edge.pytes.enums;

import io.openems.common.types.OptionsEnum;


public enum BatteryBmsStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"),
	NORMAL_COMMUNICATION(0, "Normal"),
	ABNORMAL_COMMUNICATION(1, "BMS (CAN or RS485) communication is abnormal"),
	BMS_WARNING(2, "BMS Warning (see 33145-33146 for details)"),;
	
	private final int value;
	private final String name;

	BatteryBmsStatus(int value, String name) {
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
