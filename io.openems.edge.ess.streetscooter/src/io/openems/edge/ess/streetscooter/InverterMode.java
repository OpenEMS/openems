package io.openems.edge.ess.streetscooter;

import io.openems.common.types.OptionsEnum;

public enum InverterMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(0, "Initial"), //
	WAIT(1, "Wait"), //
	START_UP(2, "Start up"), //
	NORMAL(3, "Normal"), //
	OFF_GRID(4, "Off grid"), //
	FAULT(5, "Fault"), //
	PERMANENT_FAULT(6, "Permanent fault"), //
	UPDATE_MASTER(7, "Program update of master controller"), //
	UPDATE_SLAVE(8, "Program update of slave controller");

	private final int value;
	private final String name;

	private InverterMode(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}