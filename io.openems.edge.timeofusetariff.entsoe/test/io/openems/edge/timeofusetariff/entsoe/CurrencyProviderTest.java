package io.openems.edge.timeofusetariff.entsoe;

import java.util.function.Consumer;

import io.openems.edge.common.currency.Currency;

public class CurrencyProviderTest implements CurrencyProvider {

	@Override
	public Currency getCurrent() {
		return Currency.DEFAULT;
	}

	@Override
	public void subscribe(Consumer<Currency> consumer) {
		// Empty
	}

	@Override
	public void unsubscribeAll() {
		// Empty
	}

}
