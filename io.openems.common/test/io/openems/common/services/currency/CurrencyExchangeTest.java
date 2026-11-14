package io.openems.common.services.currency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Currency;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.openems.common.utils.DoubleUtils;

class CurrencyExchangeTest {
	private static DummyCurrencyExchange exchange;

	@BeforeAll
	static void setUp() {
		exchange = new DummyCurrencyExchange();
		exchange.addBasicConversions();
	}

	@Test
	void testExchange() {
		final var eur = Currency.getInstance("EUR");
		final var usd = Currency.getInstance("USD");
		final var rub = Currency.getInstance("RUB");

		assertEquals(1.1408D, exchange.getExchangeRate(eur, usd).getAsDouble(), 0.0001);
		assertEquals(0.876577D, exchange.getExchangeRate(usd, eur).getAsDouble(), 0.0001);

		assertEquals(DoubleUtils.roundPrice(12.0 * 1.1408D), exchange.convertPrice(12.0D, eur, usd).getAsDouble(),
				0.0001);
		assertEquals(DoubleUtils.roundPrice(12.0 / 1.1408D), exchange.convertPrice(12.0, usd, eur).getAsDouble(),
				0.0001);

		assertTrue(exchange.convertPrice(12.0, eur, rub).isEmpty());
		assertTrue(exchange.getExchangeRate(eur, rub).isEmpty());

		final var hasCalledChange = new AtomicBoolean(false);
		exchange.onExchangeRatesChanged().subscribe(v -> {
			hasCalledChange.set(true);
		});
		exchange.setExchangeRate(eur, rub, 10.0);
		assertTrue(hasCalledChange.get());
	}
}
