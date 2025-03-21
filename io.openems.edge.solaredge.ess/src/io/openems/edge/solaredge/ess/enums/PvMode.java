package io.openems.edge.solaredge.ess.enums;

import io.openems.common.types.OptionsEnum;

public enum PvMode implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	
	STANDBY(0, "StandBy - not producing"), //
	ERROR(1, "Error"), //
	NO_PV(2, "No PV array detacted"), //
	PRODUCING(3, "PV is producing"), //
	LIMIT_ACTIVE(4, "PV Limitation is active"),
	WAITING(0, "Actual power not yet calculated");

	private final int value;
	private final String name;

	private PvMode(int value, String name) {
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
