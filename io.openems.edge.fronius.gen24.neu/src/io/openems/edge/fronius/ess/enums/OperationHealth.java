package io.openems.edge.fronius.ess.enums;

import io.openems.common.types.OptionsEnum;

public enum OperationHealth implements OptionsEnum {
	UNDEFINED(-1, "Undefiniert"), 
	OFF(1, "Aus"), 
	SLEEPING(2, "Standby / Nacht"),
	STARTING(3, "Startvorgang"),
	OK(4, "Alles okay (Normalbetrieb)"), 
	THROTTLED(5, "Gedrosselt (Warnung)"),
	SHUTTING_DOWN(6, "Herunterfahren"),
	ERROR(7, "Fehler (Fault)"),
	STANDBY(8, "Standby");

	private final int value;
	private final String name;

	private OperationHealth(int value, String name) {
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