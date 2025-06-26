package io.openems.common.channel;

public enum AccessMode {

	/**
	 * Read-Only.
	 */
	READ_ONLY("RO"),
	/**
	 * Read-Write.
	 */
	READ_WRITE("RW"),
	/**
	 * Write-Only.
	 */
	WRITE_ONLY("WO");

	private final String abbreviation;

	private AccessMode(String abbreviation) {
		this.abbreviation = abbreviation;
	}

	public String getAbbreviation() {
		return this.abbreviation;
	}
}
