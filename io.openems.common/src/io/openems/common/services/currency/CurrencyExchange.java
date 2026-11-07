package io.openems.common.services.currency;

import java.util.Currency;
import java.util.List;
import java.util.OptionalDouble;

import io.openems.common.event.Subscribable;

public interface CurrencyExchange {
	/**
	 * Get all stored exchange rates.
	 * 
	 * @return List copy of all exchange rates
	 */
	List<ExchangeRate> getAllExchangeRates();

	/**
	 * Converts a price from source currency to target currency.
	 * 
	 * @param price          Price to convert
	 * @param sourceCurrency Source currency
	 * @param targetCurrency Target currency
	 * @return {@link OptionalDouble#empty()} if no exchange rate exists or
	 *         Optional::of with new price
	 */
	OptionalDouble convertPrice(double price, Currency sourceCurrency, Currency targetCurrency);

	/**
	 * Returns the exchange rate from the given sourceCurrency to targetCurrency.
	 * 
	 * @param sourceCurrency Source currency
	 * @param targetCurrency Target currency
	 * @return {@link OptionalDouble#empty()} if no exchange rate exists or
	 *         Optional::of with exchange rate
	 */
	OptionalDouble getExchangeRate(Currency sourceCurrency, Currency targetCurrency);

	/**
	 * Triggers when the exchange rates are updated.
	 * 
	 * @return Event emitter that triggers when exchange rates are updated.
	 */
	Subscribable<Void> onExchangeRatesChanged();
}
