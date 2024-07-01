package io.openems.edge.ess.mr.gridcon.enums;

import io.openems.common.types.OptionsEnum;

public enum StatusIpuStateMachine implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	OFFLINE(0, "Offline"), //
	INIT(1, "Init"), //
	IDLE(2, "Idle"), //
	PRECHARGE(3, "Precharge"), //
	GO_IDLE(4, "Go idle"), // TODO are values right?
	READY(6, "Ready"), //
	RUN(7, "Run"), //
	ERROR(8, "Error"), //
	SIA(14, "SIA"), //
	FRT(15, "FRT"), //
	NOT_DEFINED(16, "Not defined");

	private final int value;
	private final String name;

	private StatusIpuStateMachine(int value, String name) {
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