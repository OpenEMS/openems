package io.openems.edge.common.currency;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import io.openems.common.types.CurrencyConfig;

class CurrencyTest {

	@Test
	void ensureAvailableJavaCurrency() {
		for (var currency : Currency.values()) {
			if (currency.isUndefined()) {
				continue;
			}
			assertNotNull(currency.getJavaCurrency(), "Java Currency not available for: " + currency.getName());
		}
	}

	@Test
	void ensureCurrencyConfigMapping() {
		for (var currencyConfig : CurrencyConfig.values()) {
			var currency = Currency.fromCurrencyConfig(currencyConfig);
			assertNotNull(currency, "Currency mapping not available for: " + currencyConfig.name());
		}
	}
}
