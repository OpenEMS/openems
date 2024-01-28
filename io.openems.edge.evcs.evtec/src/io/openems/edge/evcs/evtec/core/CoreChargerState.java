package io.openems.edge.evcs.evtec.core;

import io.openems.common.types.OptionsEnum;

public enum CoreChargerState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_READY(0, "Not ready"), //
	OPERATIONAL(1, "Operational"), //
	FAULTED(10, "Faulted") //
	;

	private final int value;
	private final String name;

	CoreChargerState(int value, String name) {
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