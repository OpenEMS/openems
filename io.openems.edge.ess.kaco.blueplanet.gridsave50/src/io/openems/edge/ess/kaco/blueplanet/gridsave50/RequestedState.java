package io.openems.edge.ess.kaco.blueplanet.gridsave50;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum RequestedState implements OptionsEnum {
	// directly addressable states
	OFF(1, "Off"), STANDBY(8, "Standby"), GRID_CONNECTED(11, "Grid connected");

	int value;
	String option;

	private RequestedState(int value, String option) {
		this.value = value;
		this.option = option;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getOption() {
		return option;
	}
}