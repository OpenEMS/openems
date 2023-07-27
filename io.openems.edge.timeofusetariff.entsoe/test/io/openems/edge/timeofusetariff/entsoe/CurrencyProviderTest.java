package io.openems.edge.timeofusetariff.entsoe;

import java.util.function.Consumer;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.currency.Currency;

public class CurrencyProviderTest implements CurrencyProvider {

	@Override
	public Currency getCurrent() {
		return Currency.DEFAULT;
	}

	@Override
	public void subscribe(Consumer<Value<Integer>> consumer) {
		// Empty
	}

	@Override
	public void unsubscribe(Consumer<Value<Integer>> consumer) {
		// Empty
	}

}
