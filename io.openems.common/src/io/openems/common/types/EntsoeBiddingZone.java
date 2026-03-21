package io.openems.common.types;

import java.util.Arrays;
import java.util.Optional;

/**
 * See <a href=
 * "https://www.entsoe.eu/data/energy-identification-codes-eic/eic-approved-codes/">entsoe</a>
 * for full list.
 */
public enum EntsoeBiddingZone {
	/**
	 * BZN|DE-LU.
	 */
	GERMANY("10Y1001A1001A82H"), //
	/**
	 * BZN|AT.
	 */
	AUSTRIA("10YAT-APG------L"), //
	/**
	 * BZN|SE1.
	 */
	SWEDEN_SE1("10Y1001A1001A44P"), //
	/**
	 * BZN|SE2.
	 */
	SWEDEN_SE2("10Y1001A1001A45N"), //
	/**
	 * BZN|SE3.
	 */
	SWEDEN_SE3("10Y1001A1001A46L"), //
	/**
	 * BZN|SE4.
	 */
	SWEDEN_SE4("10Y1001A1001A47J"), //
	/**
	 * BZN|BE.
	 */
	BELGIUM("10YBE----------2"), //
	/**
	 * BZN|NL.
	 */
	NETHERLANDS("10YNL----------L"), //
	/**
	 * BZN|CZ.
	 */
	CZECHIA("10YCZ-CEPS-----N"), //
	/**
	 * BZN|LT.
	 */
	LITHUANIA("10YLT-1001A0008Q"), //
	/**
	 * BZN|GREE.
	 */
	GREECE("10YGR-HTSO-----Y"), //

	;

	public final String code;

	private EntsoeBiddingZone(String code) {
		this.code = code;
	}

	/**
	 * Finds bidding zone by code.
	 *
	 * @param code Code to search
	 * @return Bidding zone
	 */
	public static Optional<EntsoeBiddingZone> byCode(String code) {
		return Arrays.stream(values()).filter(x -> x.code.equals(code)).findFirst();
	}
}
