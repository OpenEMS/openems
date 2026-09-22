package io.openems.edge.ess.stabl.enums;

import io.openems.common.types.OptionsEnum;

public enum ActualMainState implements OptionsEnum {

	// Init -> 0, Ready -> 1, Idle -> 2, Run -> 3, Error -> 4, Emergency -> 5,
	// Standby -> 6

	UNDEFINED(-1, "Undefined"), //
	INIT(0, "Init"), //
	READY(1, "Ready"), //
	IDLE(2, "Idle"), //
	RUN(3, "Run"), //
	ERROR(4, "Error"), //
	EMERGENCY(5, "Emergency"), //
	STANDBY(6, "StandBy")//
	;

	private final int value;
	private final String name;

	private ActualMainState(int value, String name) {
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
