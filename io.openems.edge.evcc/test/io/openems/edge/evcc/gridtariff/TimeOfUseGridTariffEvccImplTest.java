package io.openems.edge.evcc.gridtariff;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.cycleSubscriber;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.edge.common.currency.Currency.EUR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.api.UrlBuilder;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;
import io.openems.edge.predictor.api.prediction.LogVerbosity;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

public class TimeOfUseGridTariffEvccImplTest {

	@Test
	public void test() throws Exception {
		final var sut = new TimeOfUseGridTariffEvccImpl();
		final var clock = createDummyClock();
		final var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);

		// simulate response
		final ZonedDateTime now = ZonedDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
		final String jsonResponse = String.format("""
				    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
				""", now, now.plusMinutes(60));

		final var url = "http://evcc:7070/api/tariff/grid";
		final var endpointFetcher = dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {

			if (endpoint.url().equals(UrlBuilder.parse(url).toEncodedString())) {
				return HttpResponse.ok(jsonResponse);
			}

			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();

		final var factory = ofBridgeImpl(//
				() -> cycleSubscriber(), //
				() -> endpointFetcher, //
				() -> executor //
		);
		final var urlFail = "http://evcc:7070/api/tarif/grid";

		// API error
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", factory) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeofusetariff0") //
						.setApiUrl(urlFail) //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()) //

				// Case: API was called unsuccessfully
				.next(new TestCase("API error") //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> {
							executor.update();
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, sut.getPrices());
						})) //
				.deactivate();

		// API success
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", factory) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("timeofusetariff0") //
						.setApiUrl(url) //
						.setLogVerbosity(LogVerbosity.REQUESTED_PREDICTIONS) //
						.build()) //
				// Case: API was called
				.next(new TestCase("API called") //
						.timeleap(clock, 10, ChronoUnit.SECONDS) //
						.onAfterProcessImage(() -> {
							executor.update();
							TimeOfUsePrices prices = sut.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
						})) //
				.deactivate();

		// response handling (manual)
		new ComponentTest(sut) //
				.next(new TestCase("API response using old data") //
						.onBeforeProcessImage(() -> {
							// simulate response (includes invalid old data)
							ZonedDateTime past = ZonedDateTime.now(clock).withMinute(0).withSecond(0).withNano(0);
							final String jsonResponseOld = String.format(
									"""
											    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2067 },{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
											""",
									past.minusMinutes(60), now, now, now.plusMinutes(60));

							final String jsonResponsePast = String.format(
									"""
											    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2067 },{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
											""",
									past.minusMinutes(120), now.minusMinutes(60), now.minusMinutes(60), now);

							// response parsing, including old and fresh data
							var api = new TimeOfUseGridTariffEvccApi(clock);
							api.handleResponse(HttpResponse.ok(jsonResponseOld));
							TimeOfUsePrices prices = api.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);

							// no new data, keep old data cached
							api.handleResponse(HttpResponse.ok(jsonResponsePast));
							prices = api.getPrices();
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);

							// only past, never retrieved fresh data
							api = new TimeOfUseGridTariffEvccApi(clock);
							api.handleResponse(HttpResponse.ok(jsonResponsePast));
							prices = api.getPrices();
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				// invalid parsing #1
				.next(new TestCase("invalid API response") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponseFail = String.format("""
									    { "res": { "rate": [{ "begin": "%s", "end": "%s", "val": 0.2567 }]}}
									""", now, now.plusMinutes(60));

							TimeOfUsePrices prices = api.parsePrices(jsonResponseFail);
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				// invalid parsing #2
				.next(new TestCase("invalid API response") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponseFail = String.format("""
									    { "result": { "rate": [{ "begin": "%s", "end": "%s", "val": 0.2567 }]}}
									""", now, now.plusMinutes(60));

							api.parsePrices(jsonResponseFail);
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, api.getPrices());
						})) //

				// invalid parsing #3
				.next(new TestCase("invalid API response") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponseFail = String.format("""
									    { "result": { "rates": [{ "begin": "%s", "end": "%s", "val": 0.2567 }]}}
									""", now, now.plusMinutes(60));

							TimeOfUsePrices prices = api.parsePrices(jsonResponseFail);
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				// invalid interval
				.next(new TestCase("invalid interval") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponse10 = String.format("""
									    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
									""", now, now.plusMinutes(10));

							TimeOfUsePrices prices = api.parsePrices(jsonResponse10);
							assertEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
						})) //

				// 30 minute interval
				.next(new TestCase("30 minute interval") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponse30 = String.format("""
									    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
									""", now, now.plusMinutes(30));

							TimeOfUsePrices prices = api.parsePrices(jsonResponse30);
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(2, prices.asArray().length);
							assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
						})) //

				// 15 minutes interval
				.next(new TestCase("15 minute interval") //
						.onBeforeProcessImage(() -> {
							var api = new TimeOfUseGridTariffEvccApi(clock);
							String jsonResponse15 = String.format("""
									    { "result": { "rates": [{ "start": "%s", "end": "%s", "value": 0.2567 }]}}
									""", now, now.plusMinutes(15));

							TimeOfUsePrices prices = api.parsePrices(jsonResponse15);
							assertNotEquals(TimeOfUsePrices.EMPTY_PRICES, prices);
							assertEquals(1, prices.asArray().length);
							assertEquals(256.7, prices.getFirst().doubleValue(), 0.0001);
						}));
	}
}
