package io.openems.common.services.currency;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

import io.openems.common.event.EventEmitter;
import io.openems.common.event.Subscribable;

public abstract class AbstractCurrencyExchange implements CurrencyExchange {
	// Volatile: updated by a background thread (e.g. ECB fetch); read by other
	// threads
	private volatile Map<ExchangeRate.ExchangeRateKey, ExchangeRate> exchangeRates = new HashMap<>();
	private final EventEmitter<Void> exchangeRateChangeEvent = new EventEmitter<>();

	protected ExchangeRate findExchangeRate(Currency sourceCurrency, Currency targetCurrency) {
		return this.exchangeRates.get(new ExchangeRate.ExchangeRateKey(sourceCurrency, targetCurrency));
	}

	protected void setExchangeRates(List<ExchangeRate> exchangeRates) {
		this.exchangeRates = exchangeRates.stream().collect(Collectors.toMap(ExchangeRate::getKey, r -> r));
		this.exchangeRateChangeEvent.emit(null);
	}

	@Override
	public List<ExchangeRate> getAllExchangeRates() {
		return List.copyOf(this.exchangeRates.values());
	}

	@Override
	public OptionalDouble convertPrice(double price, Currency sourceCurrency, Currency targetCurrency) {
		Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
		Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");

		if (sourceCurrency.equals(targetCurrency)) {
			return OptionalDouble.of(price);
		}

		var exchangeRate = this.findExchangeRate(sourceCurrency, targetCurrency);
		if (exchangeRate != null) {
			return OptionalDouble.of(exchangeRate.convertSourcePriceToTarget(price));
		}

		exchangeRate = this.findExchangeRate(targetCurrency, sourceCurrency);
		if (exchangeRate != null) {
			return OptionalDouble.of(exchangeRate.convertTargetPriceToSource(price));
		}

		return OptionalDouble.empty();
	}

	@Override
	public OptionalDouble getExchangeRate(Currency sourceCurrency, Currency targetCurrency) {
		Objects.requireNonNull(sourceCurrency, "sourceCurrency must not be null");
		Objects.requireNonNull(targetCurrency, "targetCurrency must not be null");

		if (sourceCurrency.equals(targetCurrency)) {
			return OptionalDouble.of(1.0);
		}

		var exchangeRate = this.findExchangeRate(sourceCurrency, targetCurrency);
		if (exchangeRate != null) {
			return OptionalDouble.of(exchangeRate.getConversionRate());
		}

		exchangeRate = this.findExchangeRate(targetCurrency, sourceCurrency);
		if (exchangeRate != null) {
			return OptionalDouble.of(1.0 / exchangeRate.getConversionRate());
		}

		return OptionalDouble.empty();
	}

	@Override
	public Subscribable<Void> onExchangeRatesChanged() {
		return this.exchangeRateChangeEvent;
	}
}
