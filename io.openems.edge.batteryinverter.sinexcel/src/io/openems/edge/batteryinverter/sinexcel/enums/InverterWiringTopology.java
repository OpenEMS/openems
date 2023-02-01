package io.openems.edge.batteryinverter.sinexcel.enums;

public enum InverterWiringTopology {
	THREE_PHASE_FOUR_WIRE("3P4W"), //
	THREE_PHASE_THREE_WIRE("3P3W or 3P3W+N"); //

	private final String value;

	private InverterWiringTopology(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}