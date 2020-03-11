package io.openems.edge.ess.mr.gridcon.onoffgrid;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.ess.mr.gridcon.IState;

public enum OnOffGridState implements IState {
	UNDEFINED(-1, "Undefined"), //
	STOPPED(0, "Stopped"), //
	RUN_ONGRID(1, "RunOnGrid in on grid mode"), //
	OFFGRID(2, "Off grid mode"), //
	GOING_ONGRID(3, "Going from off grid to on grid mode"), //
	ERROR(7, "Error"), //
	;
	
	private final int value;
	private final String name;

	private OnOffGridState(int value, String name) {
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