package io.openems.edge.deye.batteryinverter.enums;

public enum FrequencyLevel {
	HZ_50("50 Hz", 0), //
	HZ_60("60 Hz", 1);//

	private final String name;
	private final int value;

	private FrequencyLevel(String name, int value) {
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