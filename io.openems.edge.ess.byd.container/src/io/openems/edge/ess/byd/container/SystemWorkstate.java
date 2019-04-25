package io.openems.edge.ess.byd.container;

import io.openems.common.types.OptionsEnum;

public enum SystemWorkstate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), 
	INITIAL(0, "Initial"), 
	FAULT(2, "Fault"), 
	STOP(4, "Stop"), 
	STANDBY(8, "Standby"),
	GRID_MONITORING(16, "Grid-Monitoring"), 
	READY(32, "Ready"),
	RUNNING(64, "Running"),
	DEBUG(128, "Debug");

	private final int value;
	private final String name;

	private SystemWorkstate(int value, String name) {
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
