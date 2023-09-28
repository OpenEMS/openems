package io.openems.edge.battery.enfasbms.enums;

import io.openems.common.types.OptionsEnum;

public enum GlobalState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	BMS_INITIALIZATION(0, "Bms initialization"), //
	BMS_IDLE_STATE(1, "BMS idle state"), //
	BMS_DISCHARGE_STATE(2, "Bms discharge state"), //
	BMS_CHARGE_STATE(3, "BMS charge state"), //
	BMS_OFFLINE_STATE(4, "Bms offline state"), //
	BMS_ERROR_STATE(5, "BMS error state"), //
	BMS_SAFE_STATE(6, "Bms safe State"), //
	BMS_SLEEP_STATE(7, "BMS sleep state"), //
	;

	private final int value;
	private final String name;

	private GlobalState(int value, String name) {
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
