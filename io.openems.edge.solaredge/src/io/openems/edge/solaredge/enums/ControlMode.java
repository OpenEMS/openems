package io.openems.edge.solaredge.enums;

import io.openems.common.types.OptionsEnum;

public enum ControlMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	SE_CTRL_MODE_NONE(0, "Disabled"), //
	SE_CTRL_MODE_MAX_SELF_CONSUMPTION(1,
			"Maximize Self Consumption – requires a SolarEdge Electricity meter on the grid or load connection point "),
	SE_CTRL_MODE_TIME_OF_USE(2,
			" Time of Use (Profile programming) – requires a SolarEdge Electricity meter on the grid or load connection point "),
	SE_CTRL_MODE_BACKUP_ONLY(3, "Backup Only (applicable only for systems support backup functionality)  "),
	SE_CTRL_MODE_REMOTE(4,
			" Remote Control – the battery charge/discharge state is controlled by an external controller  "); //

	private final int value;
	private final String name;

	private ControlMode(int value, String name) {
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
