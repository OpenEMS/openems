package io.openems.edge.pvinverter.sungrow.string;

import io.openems.common.types.OptionsEnum;

public enum WorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	RUN(0x0, "Run"), //
	STOP(0x8000, "Stop"), //
	KEY_STOP(0x1300, "Key stop"), //
	EMERGENCY_STOP(0x1500, "Emergency stop"), //
	STANDBY(0x1400, "Standby"), //
	INITIAL_STANDBY(0x1200, "Initial standby"), //
	STARTING(0x1600, "Starting"), //
	ALARM_RUN(0x9100, "Alarm run"), //
	DERATING_RUN(0x8100, "Derating run"), //
	DISPATCH_RUN(0x8200, "Dispatch run"), //
	FAULT(0x5500, "Fault"), //
	COMMUNICATE_FAULT(0x2500, "Communicate fault") //
	;

	private int value;
	private String name;

	private WorkState(int value, String name) {
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
