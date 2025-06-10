package io.openems.common.types;

import java.util.Currency;

/**
 * The {@link ChannelId#CURRENCY} mandates the selection of the 'currency'
 * configuration property of this specific type. Subsequently, this selected
 * property is transformed into the corresponding {@link Currency} type before
 * being written through {@link Meta#_setCurrency(Currency)}.
 */
public enum CurrencyConfig {
	/**
	 * Euro.
	 */
	EUR("€", "Cent", 100f),
	/**
	 * Swedish Krona.
	 */
	SEK("kr", "Öre", 100f),
	/**
	 * Swiss Francs.
	 */
	CHF("Fr", "Rappen", 100f);

	private final String symbol;

	private final String underPart;

	private final float ratio;

	private CurrencyConfig(String symbol, String underPart, float ratio) {
		this.symbol = symbol;
		this.underPart = underPart;
		this.ratio = ratio;
	}

	public String getSymbol() {
		return this.symbol;
	}

	public String getUnderPart() {
		return this.underPart;
	}

	public float getRatio() {
		return this.ratio;
	}

}
