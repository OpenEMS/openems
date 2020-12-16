package io.openems.edge.ess.mr.gridcon.state.ongrid;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.ess.mr.gridcon.IState;

public enum OnGridState implements IState {
	UNDEFINED(-1, "BASE Undefined"), //
	ONGRID(0, "BASE OnGrid"), //
	ERROR(100, "BASE Error"), //
	;

	private final int value;
	private final String name;

	private OnGridState(int value, String name) {
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