package io.openems.common.bridge.http.thirdparty;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Currency;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.bridge.http.dummy.DummyBridgeHttpBundle;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.services.currency.ExchangeRate;

class EcbCurrencyExchangeApiTest {
	private static final String TEST_ECB_RESPONSE = """
			<?xml version="1.0" encoding="UTF-8"?>
						<gesmes:Envelope xmlns:gesmes="http://www.gesmes.org/xml/2002-08-01" xmlns="http://www.ecb.int/vocabulary/2002-08-01/eurofxref">
							<gesmes:subject>Reference rates</gesmes:subject>
							<gesmes:Sender>
								<gesmes:name>European Central Bank</gesmes:name>
							</gesmes:Sender>
							<Cube>
								<Cube time='2026-07-22'>
									<Cube currency='USD' rate='1.1408'/>
									<Cube currency='JPY' rate='186.03'/>
									<Cube currency='CZK' rate='24.166'/>
									<Cube currency='DKK' rate='7.4757'/>
									<Cube currency='GBP' rate='0.85340'/>
									<Cube currency='HUF' rate='363.90'/>
									<Cube currency='PLN' rate='4.3300'/>
									<Cube currency='RON' rate='5.2380'/>
									<Cube currency='SEK' rate='11.0775'/>
									<Cube currency='CHF' rate='0.9268'/>
									<Cube currency='ISK' rate='143.20'/>
									<Cube currency='NOK' rate='10.9470'/>
									<Cube currency='TRY' rate='53.8658'/>
									<Cube currency='AUD' rate='1.6312'/>
									<Cube currency='BRL' rate='5.7949'/>
									<Cube currency='CAD' rate='1.6071'/>
									<Cube currency='CNY' rate='7.7266'/>
									<Cube currency='HKD' rate='8.9443'/>
									<Cube currency='IDR' rate='20441.71'/>
									<Cube currency='ILS' rate='3.4950'/>
									<Cube currency='INR' rate='110.1670'/>
									<Cube currency='KRW' rate='1688.78'/>
									<Cube currency='MXN' rate='19.8767'/>
									<Cube currency='MYR' rate='4.6624'/>
									<Cube currency='NZD' rate='1.9608'/>
									<Cube currency='PHP' rate='70.459'/>
									<Cube currency='SGD' rate='1.4730'/>
									<Cube currency='THB' rate='38.570'/>
									<Cube currency='ZAR' rate='18.7952'/>
								</Cube>
							</Cube>
						</gesmes:Envelope>
			""";

	@Test
	void testEcbExchange() {
		final var httpTestBundle = DummyBridgeHttpBundle.of();

		final var exchange = new EcbCurrencyExchangeApi() {
			@Override
			protected DelayTimeProvider.Delay getInitialFetchDelay() {
				return DelayTimeProvider.Delay.immediate();
			}
		};
		exchange.httpBridgeFactory = httpTestBundle.factory();
		exchange.activate();

		httpTestBundle.forceNextSuccessfulResult(HttpResponse.ok(TEST_ECB_RESPONSE));
		httpTestBundle.runTasksImmediately();

		final var eurToUsd = new ExchangeRate.ExchangeRateKey(Currency.getInstance("EUR"), Currency.getInstance("USD"));
		final var rates = exchange.getExchangeRates().getValue();
		final var eurToUsdRate = rates.stream().filter(x -> x.getKey().equals(eurToUsd)).findFirst().orElseThrow();
		assertEquals(1.1408D, eurToUsdRate.getConversionRate(), 0.0001);
	}
}
