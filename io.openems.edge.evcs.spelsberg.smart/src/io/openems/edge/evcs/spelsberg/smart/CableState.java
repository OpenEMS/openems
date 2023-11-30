package io.openems.edge.evcs.spelsberg.smart;

import io.openems.common.types.OptionsEnum;

public enum CableState implements OptionsEnum {

	UNDEFINED(-1, "Undefined"),  //
	NO_CABLE_ATTACHED(0, "No cable attached"), //
	CABLE_ATTACHED_WITHOUT_CAR(1, "Cable attached, no car attached"), //
	CABLE_ATTACHED_WITH_CAR(2, "Cable attached, car attached"), //
	CABLE_ATTACHED_WITH_CAR_AND_LOCKED(3, "Cable attached, car attached and locked"); //

	private final int value;
	private final String name;

	private CableState(int value, String name) {
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
