package io.openems.edge.ess.byd.container;

import io.openems.common.types.OptionsEnum;

public enum SystemWorkstate implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	INITIAL(0, "Initial"), //
	FAULT(1, "Fault"), //
	STOP(2, "Stop"), //
	STANDBY(3, "Standby"), //
	GRID_MONITORING(4, "Grid-Monitoring"), //
	READY(5, "Ready"), //
	RUNNING(6, "Running"), //
	DEBUG(7, "Debug");

	private final int value;
	private final String name;

	private SystemWorkstate(int value, String name) {
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
