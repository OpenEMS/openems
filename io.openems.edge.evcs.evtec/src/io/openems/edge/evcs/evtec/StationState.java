package io.openems.edge.evcs.evtec;

import io.openems.common.types.OptionsEnum;

public enum StationState implements OptionsEnum {
	UNAVAILABLE(0, "Unavailable"), //
	AVAILABLE(1, "Available"), //
	OCCUPIED(2, "Occupied"), //
	PREPARING(3, "Preparing"), //
	CHARGING(4, "Charging"), //
	FINISHING(5, "Finishing"), //
	SUSPENDED_EV(6, "Suspended EV"), //
	SUSPENDED_EVSE(7, "Suspended EVSE"), //
	NOT_READY(8, "Not ready"), //
	FAULTED(9, "Faulted"), //
	UNDEFINED(-1, "Undefined") //
	;

	private final int value;
	private final String name;

	StationState(int value, String name) {
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