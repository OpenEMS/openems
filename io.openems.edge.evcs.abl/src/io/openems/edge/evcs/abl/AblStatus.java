package io.openems.edge.evcs.abl;

import io.openems.common.types.OptionsEnum;

public enum AblStatus implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //
	BLOCKED(0x00a0, "Blocked, EVs are detected"), //
	WAITING_FOR_EV(0x00a1, "Waiting for EV"), //
	RESERVED(0x00a2, "Reserved"), //
	AUTHENTICATION_FAILED(0x00b0, "EV detected, Authentication failed"), //
	AUTHENTICATING(0x00b1, "EV detected, Authenticating"), //
	READY_FOR_CHARGING(0x00b2, "Ready for charging"), //
	CHARGING_STOPPED(0x00b3, "Charging finished or paused"), //
	CHARGING(0x00c2, "Charging"), //
	COMPLETELY_BLOCKED(0x00e0, "Blocked, EVs are not detected"), //
	BOOTING(0x00e2, "Booting"), //
	ERROR0(0x00f0, "Error 0"), //
	ERROR1(0x00f1, "Error 1"), //
	ERROR2(0x00f2, "Error 2"), //
	ERROR3(0x00f3, "Error 3"), //
	ERROR4(0x00f4, "Error 4"), //
	ERROR5(0x00f5, "Error 5"), //
	ERROR6(0x00f6, "Error 6"), //
	ERROR7(0x00f7, "Error 7"), //
	ERROR8(0x00f8, "Error 8"), //
	ERROR9(0x00f9, "Error 9"), //
	ERROR10(0x00fa, "Error 10"), //
	ERROR11(0x00fb, "Error 11"), //
	ERROR12(0x00fc, "Error 12"), //
	ERROR13(0x00fd, "Error 13"), //
	ERROR14(0x00fe, "Error 14"), //
	ERROR15(0x00ff, "Error 15"), //
	;

	private final int value;
	private final String name;

	private AblStatus(int value, String name) {
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
