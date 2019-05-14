package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import io.openems.common.types.OptionsEnum;

public enum RequestedState implements OptionsEnum {
	// directly addressable states
	OFF(1, "Off"), //
	STANDBY(8, "Standby"), //
	GRID_CONNECTED(11, "Grid connected");

	private final int value;
	private final String name;

	private RequestedState(int value, String name) {
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
		return CurrentState.UNDEFINED;
	}
}