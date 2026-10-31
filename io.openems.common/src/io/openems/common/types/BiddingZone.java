package io.openems.common.types;

import java.util.Arrays;
import java.util.Currency;
import java.util.Optional;

/**
 * See <a href=
 * "https://www.entsoe.eu/data/energy-identification-codes-eic/eic-approved-codes/">entsoe</a>
 * for full list.
 */
public enum BiddingZone {
	/**
	 * BZN|DE-LU.
	 */
	GERMANY("10Y1001A1001A82H", "EUR"), //
	/**
	 * BZN|AT.
	 */
	AUSTRIA("10YAT-APG------L", "EUR"), //
	/**
	 * BZN|SE1.
	 */
	SWEDEN_SE1("10Y1001A1001A44P", "SEK"), //
	/**
	 * BZN|SE2.
	 */
	SWEDEN_SE2("10Y1001A1001A45N", "SEK"), //
	/**
	 * BZN|SE3.
	 */
	SWEDEN_SE3("10Y1001A1001A46L", "SEK"), //
	/**
	 * BZN|SE4.
	 */
	SWEDEN_SE4("10Y1001A1001A47J", "SEK"), //
	/**
	 * BZN|BE.
	 */
	BELGIUM("10YBE----------2", "EUR"), //
	/**
	 * BZN|NL.
	 */
	NETHERLANDS("10YNL----------L", "EUR"), //
	/**
	 * BZN|CZ.
	 */
	CZECHIA("10YCZ-CEPS-----N", "EUR"), //
	/**
	 * BZN|LT.
	 */
	LITHUANIA("10YLT-1001A0008Q", "EUR"), //
	/**
	 * BZN|GREE.
	 */
	GREECE("10YGR-HTSO-----Y", "EUR"), //

	;

	private final String eicCode;
	private final Currency defaultCurrency;

	private BiddingZone(String eicCode, String defaultCurrencyCode) {
		this.eicCode = eicCode;
		this.defaultCurrency = Currency.getInstance(defaultCurrencyCode);
	}

	public String getEicCode() {
		return this.eicCode;
	}

	public Currency getDefaultCurrency() {
		return this.defaultCurrency;
	}

	/**
	 * Finds bidding zone by code.
	 *
	 * @param code Code to search
	 * @return Bidding zone
	 */
	public static Optional<BiddingZone> byEicCode(String code) {
		return Arrays.stream(values()).filter(x -> x.eicCode.equals(code)).findFirst();
	}
}
