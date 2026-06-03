package io.openems.edge.fronius.enums;

import io.openems.common.types.OptionsEnum;

public enum BatteryState implements OptionsEnum {
	UNDEFINED(-1, "Undefiniert"), 
	OFF(1, "Aus"), 
	EMPTY(2, "Leer"),
	DISCHARGING(3, "Entladen"), 
	CHARGING(4, "Laden"), 
	FULL(5, "Voll"),
	STANDBY(6, "Standby / Holding"),
	TESTING(7, "Testmodus");

	private final int value;
	private final String name;

	private BatteryState(int value, String name) {
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