package io.openems.edge.ess.mr.gridcon.state.onoffgrid;

import io.openems.common.types.OptionsEnum;
import io.openems.edge.ess.mr.gridcon.IState;

public enum OnOffGridState implements IState {
	UNDEFINED(-1, "Undefined"), //
	START_SYSTEM(100, "Start System"), //
	WAITING_FOR_DEVICES(101, "Waiting until devices are online"), 
	ON_GRID_MODE(102, "On Grid Mode"),
	OFF_GRID_MODE(200, "Off Grid Mode"),
	OFF_GRID_MODE_GRID_BACK(201, "Off Grid Mode - Grid is back"),
	OFF_GRID_MODE_WAIT_FOR_GRID_AVAILABLE(202, "Off Grid Mode - Grid is back"),	
	OFF_GRID_MODE_ADJUST_PARMETER(203, "Off Grid Mode - Adjust Parameter for Synchronisation"),	
	ON_GRID_RESTART_GRIDCON_AFTER_SYNC(103, "On Grid - Restart Gridcon After Synchronisation"),
	ERROR(300, "Error"),
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