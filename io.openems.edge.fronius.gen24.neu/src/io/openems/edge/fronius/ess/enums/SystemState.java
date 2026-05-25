package io.openems.edge.fronius.ess.enums;

import io.openems.common.types.OptionsEnum;

public enum SystemState implements OptionsEnum {
	UNDEFINED(-1, "Undefiniert"),
	OFF(1, "Aus"),
	SLEEPING(2, "Standby/Nacht"),
	STARTING(3, "Startvorgang"),
	OK(4, "Normalbetrieb"),
	THROTTLED(5, "Leistungsbegrenzung"),
	SHUTTING_DOWN(6, "Herunterfahren"),
	FAULT(7, "Fehler"),
	STANDBY(8, "Standby");

	private final int value;
	private final String name;

	private SystemState(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() { return this.value; }

	@Override
	public String getName() { return this.name; }

	@Override
	public OptionsEnum getUndefined() { return UNDEFINED; }
}