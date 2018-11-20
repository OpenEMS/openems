package io.openems.edge.ess.fenecon.commercial40;

import io.openems.edge.common.channel.doc.OptionsEnum;

public enum SetWorkState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	START(4, "Start"), //
	STANDBY(32, "Standby"), //
	STOP(64, "Stop");

	private final int value;
	private final String name;

	private SetWorkState(int value, String name) {
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