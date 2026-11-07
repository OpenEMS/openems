package io.openems.common.services.currency;

import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

public class DummyCurrencyExchange extends AbstractCurrencyExchange {
	private final Map<ExchangeRate.ExchangeRateKey, ExchangeRate> exchangeRates = new HashMap<>();

	public DummyCurrencyExchange() {
		this.update();
	}

	private void update() {
		this.setExchangeRates(List.copyOf(this.exchangeRates.values()));
	}

	public void setExchangeRate(Currency sourceCurrency, Currency targetCurrency, double conversionRate) {
		var key = new ExchangeRate.ExchangeRateKey(sourceCurrency, targetCurrency);
		this.exchangeRates.put(key, new ExchangeRate(key, conversionRate));
		this.update();
	}

	/**
	 * Adds conversions rates from EUR to a lot of different currencies. The
	 * conversions rates are the real conversions rates from 2026-07-28.
	 */
	public void addBasicConversions() {
		var builder = ImmutableMap.<String, Double>builder();
		builder.put("USD", 1.1408D);
		builder.put("JPY", 186.03D);
		builder.put("CZK", 24.166D);
		builder.put("DKK", 7.4757D);
		builder.put("GBP", 0.85340D);
		builder.put("HUF", 363.90D);
		builder.put("PLN", 4.3300D);
		builder.put("RON", 5.2380D);
		builder.put("SEK", 11.0775D);
		builder.put("CHF", 0.9268D);
		builder.put("ISK", 143.20D);
		builder.put("NOK", 10.9470D);
		builder.put("TRY", 53.8658D);
		builder.put("AUD", 1.6312D);
		builder.put("BRL", 5.7949D);
		builder.put("CAD", 1.6071D);
		builder.put("CNY", 7.7266D);
		builder.put("HKD", 8.9443D);
		builder.put("IDR", 20441.71D);
		builder.put("ILS", 3.4950D);
		builder.put("INR", 110.1670D);
		builder.put("KRW", 1688.78D);
		builder.put("MXN", 19.8767D);
		builder.put("MYR", 4.6624D);
		builder.put("NZD", 1.9608D);
		builder.put("PHP", 70.459D);
		builder.put("SGD", 1.4730D);
		builder.put("THB", 38.570D);
		builder.put("ZAR", 18.7952D);

		final var eur = Currency.getInstance("EUR");
		for (var entry : builder.build().entrySet()) {
			var key = new ExchangeRate.ExchangeRateKey(eur, Currency.getInstance(entry.getKey()));
			this.exchangeRates.put(key, new ExchangeRate(key, entry.getValue()));
		}

		this.update();
	}
}
