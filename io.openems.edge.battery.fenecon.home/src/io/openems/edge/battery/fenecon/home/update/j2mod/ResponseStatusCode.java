package io.openems.edge.battery.fenecon.home.update.j2mod;

import java.util.Optional;

/**
 * The status code returned from the BMS. The status code list is shared between
 * all FC40 - FC44 requests. The ordinal is identical to the byte status code.
 */
public enum ResponseStatusCode {
	WAIT, //
	OK, //
	FLASH_ERROR, //
	FILE_ERROR, //
	ERROR_DURING_UPDATE, //
	UPDATE_FINISHED, //

	;

	public int getStatusCode() {
		return this.ordinal();
	}

	/**
	 * Parses the given status code into a {@link ResponseStatusCode}. Returns
	 * Optional.empty() if status code is invalid.
	 * 
	 * @param statusCode Status code to convert
	 * @return Optional with {@link ResponseStatusCode} value or Optional.empty()
	 */
	public static Optional<ResponseStatusCode> byStatusCode(int statusCode) {
		if (statusCode < 0 || statusCode >= values().length) {
			return Optional.empty();
		}

		return Optional.of(values()[statusCode]);
	}
}
