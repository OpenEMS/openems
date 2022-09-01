package io.openems.edge.battery.fenecon.commercial;

public enum Baudrate {
	BAUDRATE_9600("9600", 0), //
	BAUDRATE_19200("19200", 100), //
	BAUDRATE_38400("38400", 200),//
	;//

	private final String name;
	private final int value;

	private Baudrate(String name, int value) {
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