package io.openems.edge.simulator;

public enum CsvFormat {
	GERMAN_EXCEL(";", ","), //
	ENGLISH(",", ".");

	public final String lineSeparator;
	public final String decimalSeparator;

	private CsvFormat(String lineSeparator, String decimalSeparator) {
		this.lineSeparator = lineSeparator;
		this.decimalSeparator = decimalSeparator;
	}
}
