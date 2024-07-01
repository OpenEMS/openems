package io.openems.edge.fenecon.pro.ess;

import io.openems.common.types.OptionsEnum;

public enum PcsMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	EMERGENCY(0, "Emergency"), //
	CONSUMERS_PEAK_PATTERN(1, "Consumers Peak Pattern"), //
	ECONOMIC(2, "Economic"), //
	ECO(3, "Eco"), //
	DEBUG(4, "Debug"), //
	SMOOTH_PV(5, "Smooth PV"), //
	REMOTE(6, "Remote");

	private final int value;
	private final String name;

	private PcsMode(int value, String name) {
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