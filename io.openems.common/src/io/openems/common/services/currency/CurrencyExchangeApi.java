package io.openems.common.services.currency;

import java.util.List;

import io.openems.common.utils.BehaviorSubject;

public interface CurrencyExchangeApi {
	/**
	 * Get all stored exchange rates.
	 *
	 * @return List copy of all exchange rates
	 */
	BehaviorSubject<List<ExchangeRate>> getExchangeRates();
}
