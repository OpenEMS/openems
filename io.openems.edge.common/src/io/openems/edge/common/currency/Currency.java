package io.openems.edge.common.currency;

import io.openems.common.types.CurrencyConfig;
import io.openems.common.types.OptionsEnum;

public enum Currency implements OptionsEnum {
	UNDEFINED(-1), //
	EUR(0), //
	SEK(1), //
	CHF(2), //
	;

	private final int value;

	private Currency(int value) {
		this.value = value;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.name();
	}

	@Override
	public OptionsEnum getUndefined() {
		return Currency.UNDEFINED;
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
	
}
