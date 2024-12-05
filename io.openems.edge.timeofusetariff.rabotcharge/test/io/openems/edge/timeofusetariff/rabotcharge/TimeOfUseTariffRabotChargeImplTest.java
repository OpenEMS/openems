package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.timeofusetariff.rabotcharge.TimeOfUseTariffRabotChargeImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.bridge.http.api.HttpError;
import io.openems.edge.bridge.http.api.HttpResponse;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.timeofusetariff.rabotcharge.TimeOfUseTariffRabotChargeImpl.PriceComponents;

public class TimeOfUseTariffRabotChargeImplTest {

	private static final String DUMMY_PRICES = """
				{
			    "records": [
			        {
			            "timestamp": "2024-05-14T09:00:00+02:00",
			            "priceInCentPerKwh": 0.564
			        },
			        {
			            "timestamp": "2024-05-14T10:00:00+02:00",
			            "priceInCentPerKwh": -0.233
			        },
			        {
			            "timestamp": "2024-05-14T11:00:00+02:00",
			            "priceInCentPerKwh": -1.112
			        },
			        {
			            "timestamp": "2024-05-14T12:00:00+02:00",
			            "priceInCentPerKwh": -3.633
			        },
			        {
			            "timestamp": "2024-05-14T13:00:00+02:00",
			            "priceInCentPerKwh": -4.295
			        },
			        {
			            "timestamp": "2024-05-14T14:00:00+02:00",
			            "priceInCentPerKwh": -3.966
			        },
			        {
			            "timestamp": "2024-05-14T15:00:00+02:00",
			            "priceInCentPerKwh": -1.986
			        },
			        {
			            "timestamp": "2024-05-14T16:00:00+02:00",
			            "priceInCentPerKwh": -0.293
			        },
			        {
			            "timestamp": "2024-05-14T17:00:00+02:00",
			            "priceInCentPerKwh": -0.004
			        },
			        {
			            "timestamp": "2024-05-14T18:00:00+02:00",
			            "priceInCentPerKwh": 4.374
			        },
			        {
			            "timestamp": "2024-05-14T19:00:00+02:00",
			            "priceInCentPerKwh": 8.390
			        },
			        {
			            "timestamp": "2024-05-14T20:00:00+02:00",
			            "priceInCentPerKwh": 9.347
			        },
			        {
			            "timestamp": "2024-05-14T21:00:00+02:00",
			            "priceInCentPerKwh": 7.641
			        },
			        {
			            "timestamp": "2024-05-14T22:00:00+02:00",
			            "priceInCentPerKwh": 4.535
			        },
			        {
			            "timestamp": "2024-05-14T23:00:00+02:00",
			            "priceInCentPerKwh": 2.771
			        }
			    ]
			}""";

	private static final PriceComponents PRICE_COMPONENTS = new PriceComponents(5.0611, 9.3415, 6.4627);

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();

		final var endpointFetcher = DummyBridgeHttpFactory.dummyEndpointFetcher();
		endpointFetcher.addEndpointHandler(endpoint -> {

			if (endpoint.url().equals(TimeOfUseTariffRabotChargeImpl.RABOT_CHARGE_TOKEN_URL.toEncodedString())) {
				return HttpResponse.ok(JsonUtils.buildJsonObject() //
						.addProperty("access_token", "FJAWognawn") //
						.build().toString());
			}

			if (endpoint.url().equals(TimeOfUseTariffRabotChargeImpl.RABOT_CHARGE_API_URL.toEncodedString())) {
				return HttpResponse.ok(DUMMY_PRICES);
			}

			if (endpoint.url()
					.startsWith(TimeOfUseTariffRabotChargeImpl.RABOT_CHARGE_PRIZE_COMPONENT_URL.toEncodedString())) {
				return HttpResponse.ok(JsonUtils.buildJsonObject() //
						.addProperty("taxAndFeeKwHPrice", 3.44) //
						.addProperty("gridFeeKwHPrice", 7.89) //
						.addProperty("gridFeeFixed", 5.22) //
						.build().toString());
			}

			throw HttpError.ResponseError.notFound();
		});

		final var executor = DummyBridgeHttpFactory.dummyBridgeHttpExecutor();

		final var factory = DummyBridgeHttpFactory.ofBridgeImpl(//
				() -> DummyBridgeHttpFactory.cycleSubscriber(), //
				() -> endpointFetcher, //
				() -> executor //
		);

		final var sut = new TimeOfUseTariffRabotChargeImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", factory) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setZipcode("00000000") //
						.setClientId("clientId") //
						.setClientSecret("clientSecret") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							// get auth token
							executor.update();

							// get prices
							executor.update();
						}) //
						.output(new ChannelAddress("ctrl0", "HttpStatusCode"), HttpStatus.OK.code()));
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(DUMMY_PRICES, PRICE_COMPONENTS); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(214.2929, prices.getFirst(), 0.001);
	}

	@Test
	public void emptyStringTest() throws OpenemsNamedException {
		assertThrows(OpenemsNamedException.class, () -> {
			parsePrices("", PRICE_COMPONENTS);
		});
	}
}
