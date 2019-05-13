package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import io.openems.common.types.OptionsEnum;

public enum CurrentState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFF(1, "Off"), // directly addressable
	STANDBY(8, "Standby"), // directly addressable
	GRID_CONNECTED(11, "Grid connected"), // directly addressable
	ERROR(7, "Error"), // can be reached from every state, not directly addressable
	PRECHARGE(9, "Precharge"), // State when system goes from OFF to STANDBY, not directly addressable
	STARTING(3, "Starting"), // State from STANDBY to GRID_CONNECTED, not directly addressable
	SHUTTING_DOWN(6, "Shutting down"), // State when system goes from GRID_CONNECTED to STANDBY, not directly
										// addressable
	CURRENTLY_UNKNOWN(10, "Currently not known"), // State appears sometimes, but currently there exists no
													// documentation
	NO_ERROR_PENDING(12, "No error pending"), // State when system goes from ERROR to OFF, not directly addressable
	THROTTLED(5, "Throttled"); // State that can occur when system is GRID_CONNECTED, not directly addressable

	private final int value;
	private final String name;

	private CurrentState(int value, String name) {
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