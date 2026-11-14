package io.openems.edge.common.currency;

import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.OptionsEnum;

/**
 * Enum representing currencies by their ISO 4217 currency codes. Each enum
 * constant name corresponds exactly to the 3-letter ISO 4217 code.
 */
public enum Currency implements OptionsEnum {
	UNDEFINED(-1, null), //
	EUR(0, findJavaCurrency("EUR")), //
	SEK(1, findJavaCurrency("SEK")), //
	CHF(2, findJavaCurrency("CHF")), //
	;

	private static java.util.Currency findJavaCurrency(String currencyCode) {
		return java.util.Currency.getInstance(currencyCode);
	}

	private final int value;
	private final java.util.Currency javaCurrency;

	private Currency(int value, java.util.Currency javaCurrency) {
		this.value = value;
		this.javaCurrency = javaCurrency;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	/**
	 * Returns the ISO 4217 currency code as the name of the enum constant.
	 * 
	 * @return The ISO 4217 currency code (e.g., "EUR", "CHF").
	 */
	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return Currency.UNDEFINED;
	}

	/**
	 * Returns the corresponding Java currency.
	 *
	 * @return the Java currency, or {@code null} for {@link #UNDEFINED}
	 */
	public java.util.Currency getJavaCurrency() {
		return this.javaCurrency;
	}

	/**
	 * Converts the {@link CurrencyConfig} to the {@link Currency}.
	 * 
	 * @param config currencyConfig to be transformed
	 * @return The {@link Currency}.
	 */
	public static Currency fromCurrencyConfig(CurrencyConfig config) {
		return switch (config) {
		case EUR -> Currency.EUR;
		case SEK -> Currency.SEK;
		case CHF -> Currency.CHF;
		};
	}

	/**
	 * Returns the {@link Currency} enum constant matching the given ISO 4217
	 * currency code. If the code is null or does not match any known currency,
	 * {@link #UNDEFINED} is returned.
	 * 
	 * @param code the ISO 4217 currency code (case-insensitive)
	 * @return the matching {@link Currency} enum constant or {@link #UNDEFINED} if
	 *         none matches
	 */
	public static Currency fromCode(String code) {
		if (code == null) {
			return UNDEFINED;
		}
		try {
			return Currency.valueOf(code.toUpperCase());
		} catch (IllegalArgumentException e) {
			return UNDEFINED;
		}
	}
}
