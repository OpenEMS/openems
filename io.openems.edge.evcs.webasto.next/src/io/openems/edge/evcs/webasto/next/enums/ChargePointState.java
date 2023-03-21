package io.openems.edge.evcs.webasto.next.enums;

import io.openems.common.types.OptionsEnum;

public enum ChargePointState implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NO_VEHICLE_ATTACHED(0, "No vehicle attached"), //
	NO_PERMISSION(1, "Vehicle attached, no permission (preparing)"), //
	CHARGING(3, "Charging"), //
	CHARGING_PAUSED(4, "Charging paused"), //
	ERROR(7, "Charging error"), //
	CHARGING_STATION_RESERVED(8, "Charging station reserved") //
	;

	private final int value;
	private final String name;

	private ChargePointState(int value, String name) {
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
