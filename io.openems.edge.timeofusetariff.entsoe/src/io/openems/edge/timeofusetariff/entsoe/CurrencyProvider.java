package io.openems.edge.timeofusetariff.entsoe;

import java.util.function.Consumer;

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
	public void subscribe(Consumer<Currency> consumer);

	/**
	 * Unsubscribes from all the Subscriptions.
	 */
	public void unsubscribeAll();

}
