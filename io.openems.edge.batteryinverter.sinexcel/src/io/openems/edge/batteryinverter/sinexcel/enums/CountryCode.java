package io.openems.edge.batteryinverter.sinexcel.enums;

public enum CountryCode {

	GERMANY("Germany"), //
	AUSTRIA("Austria"), //
	SWITZERLAND("Switzerland");

	private final String value;

	private CountryCode(String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}