package io.openems.edge.aircooling.blueeplus;

import io.openems.common.types.OptionsEnum;

public enum StatusValues implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	NOT_AVAILABLE(1, "Not available"), //
	CONFIG_CHANGED(2, "configChanged"), //
	ERROR(3, "error"), //
	OK(4, "ok"), //
	ALARM(5, "alarm"), //
	HIGH_WARN(6, "highWarn"), //
	LOW_ALARM(7, "lowAlarm"), //
	HIGH_ALARM(8, "highAlarm"), //
	LOW_WARN(9, "lowWarn"), //
	SET_OFF(10, "setOff"), //
	SET_ON(11, "setOn"), //
	OPEN(12, "open"), //
	CLOSED(13, "closed"), //
	LOCKED(14, "locked"), //
	UNLOCK_REMOTE(15, "unlRemote"), //
	DOOR_OPEN(16, "doorOpen"), //
	SERVICE(17, "service"), //
	STANDBY(18, "standby"), //
	BUSY(19, "busy"), //
	NO_ACCESS(20, "noAccess"), //
	LOST(21, "lost"), //
	DETECTED(22, "detected"), //
	LOW_VOLTAGE(23, "lowVoltage"), //
	PROBE_OPEN(24, "probeopen"), //
	PROBE_SHORT(25, "probeshort"), //
	CALIBRATION(26, "calibration"), //
	INACTIVE(27, "inactive"), //
	ACTIVE(28, "active"), //
	NO_POWER(29, "noPower"), //
	READ_ONLY(30, "readOnly"), //
	EXCHANGED(31, "exchanged"), //
	VALVE_OPEN(32, "valveOpen"), //
	WARNING(33, "warning"), //
	REMOTE(34, "remote"),//
	;

	private final int value;
	private final String name;

	private StatusValues(int value, String name) {
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
