package io.openems.edge.timeofusetariff.entsoe;

import java.util.function.Consumer;

import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.currency.Currency;

public interface CurrencyProvider {

	/**
	 * Returns the current Currency.
	 * 
	 * @return The {@link Currency}
	 */
	public Currency getCurrent();

	/**
	 * Subscribes to the Currency channel to trigger on update.
	 * 
	 * @param consumer The callback {@link Consumer}.
	 */
	public void subscribe(Consumer<Value<Integer>> consumer);

	/**
	 * Unsubscribe from the Currency channel.
	 * 
	 * @param consumer The callback {@link Consumer}.
	 */
	public void unsubscribe(Consumer<Value<Integer>> consumer);

}
