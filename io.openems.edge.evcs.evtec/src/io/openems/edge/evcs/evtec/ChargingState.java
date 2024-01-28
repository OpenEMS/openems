package io.openems.edge.evcs.evtec;

import io.openems.common.types.OptionsEnum;

public enum ChargingState implements OptionsEnum {
	NO_VEHICLE_CONNECTED(0, "Charging process not started (no vehicle connected)"), //
	WAITING_FOR_RELEASE(1, "Connected, waiting for release (by RFID or Local)"), //
	CHARGING_PROCESS_STARTS(2, "Charging process starts"), //
	STOP(3, "Stop"), //
	SUSPENDED(4, "Suspended (loading paused)"), //
	CHARGING_PROCESS_SUCCESSFULLY_COMPLETED(5, "Charging process successfully completed (vehicle still plugged in)"), //
	CHARGING_PROCESS_COMPLETED_BY_USER(6, "Charging process completed by user (vehicle still plugged in)"), //
	CHARGING_ENDED_WITH_ERROR(7, "Charging ended with error (vehicle still connected)"), //
	UNDEFINED(-1, "Undefined") //
	;

	private final int value;
	private final String name;

	ChargingState(int value, String name) {
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