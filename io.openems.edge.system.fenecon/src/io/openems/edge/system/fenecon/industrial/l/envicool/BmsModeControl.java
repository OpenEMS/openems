package io.openems.edge.system.fenecon.industrial.l.envicool;

import io.openems.common.types.OptionsEnum;

public enum BmsModeControl implements OptionsEnum {

	UNDEFINED(-1, "Undefined"), //
	FULLY_AUTOMATIC(0, "Fully automatic"), //
	MANUAL_COOLING(1, "Manual cooling"), //
	MANUAL_HEATING(2, "Manual heating"), //
	SELF_CIRCULATION(3, "Self circulation"), //
	STOP(4, "Stop"), //
	;

	private final int value;
	private final String name;

	private BmsModeControl(int value, String name) {
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
