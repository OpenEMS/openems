package io.openems.edge.batteryinverter.sinexcel.enums;

public enum VoltageLevel {
	V_380("380 V", 0), //
	V_400("400 V", 1), //
	V_480("480 V", 2); //

	private final String name;
	private final int value;

	private VoltageLevel(String name, int value) {
		this.name = name;
		this.value = value;
	}

	public String getName() {
		return this.name;
	}

	public int getValue() {
		return this.value;
	}
}
