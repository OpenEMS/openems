package io.openems.edge.batteryinverter.refu88k.enums;

import io.openems.common.types.OptionsEnum;

public enum LocRemCtl implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	REMOTE(0, "PCS Remote Control"), //
	LOCAL(1, "PCS Local Control"),;

	private final int value;
	private final String name;

	private LocRemCtl(int value, String name) {
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
