package io.openems.edge.ess.sungrow.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	STOP(0x0002, "Stop"), //
	STANDBY(0x0008, "Standby"), //
	INITIAL_STANDBY(0x0010, "Initial Standby"), //
	STARTUP(0x0020, "Startup"), //
	RUNNING(0x0040, "Running"), //
	FAULT(0x0100, "Fault"), //
	RUNNING_IN_MAINTAIN_MODE(0x0400, "Running in maintain mode"), //
	RUNNING_IN_FORCED_MODE(0x0800, "Running in forced mode"), //
	// RUNNING_IN_OFF_GRID_MODE(0x0400, "Running in off-grid mode"), //
	RESTARTING(0x2501, "Restarting"), //
	RUNNING_IN_EXTERNAL_EMS_MODE(0x4000, "Running in External EMS mode") //
	;

	private final int value;
	private final String name;

	private SystemState(int value, String name) {
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
