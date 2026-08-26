package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyBridgeHttpExecutor;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.dummyEndpointFetcher;
import static io.openems.common.bridge.http.dummy.DummyBridgeHttpFactory.ofBridgeImpl;
import static io.openems.common.test.TestUtils.createDummyClock;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import io.openems.common.bridge.http.api.HttpError;
import io.openems.common.bridge.http.api.HttpResponse;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.HttpStatus;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.timeofusetariff.rabotcharge.RabotChargeApi.PriceComponents;
import io.openems.edge.common.test.DummyOAuthBackend;

public class TimeOfUseTariffRabotChargeImplTest {

	private static final String SAMPLE_CUSTOMERS_JSON = """
			{
			    "count": 1,
			    "hasMoreItems": false,
			    "data": [
			        {
			            "customerNumber": "75741496",
			            "externalIdentifier": null,
			            "firstName": "Loris",
			            "lastName": "Wagner",
			            "businessName": null,
			            "emailAddress": "test_loris.wagner@rabot-charge.de",
			            "relations": [
			                "Linked"
			            ]
			        }
			    ],
			    "isSuccess": true,
			    "message": null,
			    "error": null
			}
			""";

	private static final String SAMPLE_CONTRACTS_JSON = """
			{
			    "count": 1,
			    "hasMoreItems": false,
			    "data": [
			        {
			            "contractNumber": "15333823",
			            "tariffName": "rabot.home",
			            "contractState": "Delivery",
			            "deliveryAddress": {
			                "firstName": "Joscha",
			                "lastName": "Pletsch",
			                "streetName": "Max-Liebermann-Str.",
			                "houseNumber": "1",
			                "city": "Jennyland",
			                "postCode": "10405"
			            },
			            "meterNumber": "4TEST21394713",
			            "actualDateOfDelivery": "2024-08-20"
			        }
			    ],
			    "isSuccess": true,
			    "message": null,
			    "error": null
			}
			""";

	private static final String SAMPLE_COSTS_JSON = """
			{
			    "data": {
			        "validFrom": "2026-05-01 00:00",
			        "amountKind": "Gross",
			        "baseServiceFee": { "value": 4.9900 },
			        "variableServiceFee": { "value": 1.7850 },
			        "baseGridFee": { "value": 3.3082 },
			        "variableGridFee": { "value": 11.8643 },
			        "meteringFee": { "value": 1.0740 },
			        "expectedEnergyCost": { "value": 12.3248 },
			        "guaranteeOfOrigin": { "value": 0.5355 },
			        "concessionFee": { "value": 2.8441 },
			        "kwkgFee": { "value": 0.3296 },
			        "electricityTax": { "value": 2.4395 },
			        "savingsShare": { "value": 0.0000 },
			        "estimatedConsumption": { "value": 125.0000 },
			        "offshoreFee": { "value": 0.9710 },
			        "p19Fee": { "value": 1.8540 },
			        "exchangeFee": { "value": 0.0357 },
			        "savingToComparisonTariff": { "value": 8.6976 }
			    },
			    "isSuccess": true,
			    "message": null,
			    "error": null
			}
			""";

	private static final String SAMPLE_MARKET_PRICES_JSON = """
			{
			    "data": {
			        "pricesUnit": {
			            "main": "Cent",
			            "per": "Kwh"
			        },
			        "pricesKind": "Net",
			        "from": "2026-05-19 00:00",
			        "to": "2026-05-21 00:00",
			        "prices": [
			            {
			                "at": "2026-05-19 00:00",
			                "price": 15.9630
			            },
			            {
			                "at": "2026-05-19 00:15",
			                "price": 14.8470
			            },
			            {
			                "at": "2026-05-19 00:30",
			                "price": 14.3890
			            }
			        ]
			    },
			    "isSuccess": true,
			    "message": null,
			    "error": null
			}
			""";

	@Test
	public void testParseCostsJson() throws Exception {
		var jsonObject = JsonUtils.parseToJsonObject(SAMPLE_COSTS_JSON);

		// Deserialize using the corrected serializer definition
		var priceComponents = RabotChargeApi.PriceComponents.serializer().deserialize(jsonObject);

		assertNotNull(priceComponents);

		// Assert individual fields were reached inside their nested { "value": ... }
		// structures
		assertEquals(11.8643, priceComponents.variableGridFee(), 0.0001);
		assertEquals(2.8441, priceComponents.concessionFee(), 0.0001);
		assertEquals(0.3296, priceComponents.kwkgFee(), 0.0001);
		assertEquals(2.4395, priceComponents.electricityTax(), 0.0001);
		assertEquals(0.9710, priceComponents.offshoreFee(), 0.0001);
		assertEquals(1.8540, priceComponents.p19Fee(), 0.0001);
		assertEquals(0.0357, priceComponents.exchangeFee(), 0.0001);
		assertEquals(0.5355, priceComponents.guaranteeOfOrigin(), 0.0001);
		assertEquals(1.7850, priceComponents.variableServiceFee(), 0.0001);

		// Expected sum calculation:
		// 11.8643 + 2.8441 + 0.3296 + 2.4395 + 0.9710 + 1.8540 + 0.0357 + 0.5355 +
		// 1.7850 = 22.6587
		double expectedGrossVariableFees = 22.6587;
		assertEquals(expectedGrossVariableFees, priceComponents.getVariableFeesGross(), 0.0001);
	}

	@Test
	public void testParsePricesCalculation() throws Exception {
		var costs = new PriceComponents(11.8643, 2.8441, 0.3296, 2.4395, 0.9710, 1.8540, 0.0357, 0.5355, 1.7850);
		var timeOfUsePrices = TimeOfUseTariffRabotChargeImpl.parsePrices(SAMPLE_MARKET_PRICES_JSON, costs);

		assertNotNull(timeOfUsePrices);

		// Base Day-Ahead Price = 15.9630 ct/kWh (Net)
		// Day-Ahead Gross (with 19% VAT) = 15.9630 * 1.19 = 18.99597 ct/kWh
		// Constant Variable Fees (Gross) = 22.6587 ct/kWh
		// Total Consumer Price (ct/kWh) = 18.99597 + 22.6587 = 41.65467 ct/kWh
		// Conversion to Base Price Unit (EUR/MWh) = 41.65467 * 10 = 416.5467 EUR/MWh

		double expectedEurPerMwh = 416.5467;

		var pricesMap = timeOfUsePrices.toMap();

		// Ensure we parsed all 3 items from the trimmed JSON array
		assertEquals(3, pricesMap.size());

		// Get the first chronologically ordered value
		var firstCalculatedValue = pricesMap.values().stream().findFirst().orElse(0.0);

		assertEquals(expectedEurPerMwh, firstCalculatedValue, 0.001);
	}

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		final var endpointFetcher = dummyEndpointFetcher();

		endpointFetcher.addEndpointHandler(endpoint -> {
			final var url = endpoint.url();

			// Mock Token Endpoint
			if (url.contains("/connect/token")) {
				return HttpResponse.ok(JsonUtils.buildJsonObject()
						.addProperty("access_token", "mock_partner_token_xyz")
						.addProperty("refresh_token", "dummy_refresh_token_value") // Required for OpenEMS standard parsing
						.addProperty("expires_in", 3600)
						.build().toString());
			}

			// Mock Customer Link Endpoint (REQUIRED TO AVOID DEADLOCK)
			if (url.contains("/customers/link")) {
				return HttpResponse.ok(JsonUtils.buildJsonObject()
						.addProperty("authorizationUrl", "https://mock-rabot-auth-url")
						.build().toString());
			}
			
			if (url.endsWith("/customers") || url.endsWith("/customers/")) {
				return HttpResponse.ok(SAMPLE_CUSTOMERS_JSON);
			}

			// Mock Contracts Endpoint
			if (url.contains("/contracts") && !url.contains("/costs")) {
				return HttpResponse.ok(SAMPLE_CONTRACTS_JSON);
			}

			// Mock Costs Endpoint
			if (url.contains("/costs")) {
				return HttpResponse.ok(SAMPLE_COSTS_JSON);
			}

			// Mock Prices Endpoint
			if (url.contains("/day-ahead-prices/limited")) {
				return HttpResponse.ok(SAMPLE_MARKET_PRICES_JSON);
			}

			throw HttpError.ResponseError.notFound();
		});

		final var executor = dummyBridgeHttpExecutor();
		final var factory = ofBridgeImpl(//
				() -> endpointFetcher, //
				() -> executor //
		);

		final var sut = new TimeOfUseTariffRabotChargeImpl();
		new ComponentTest(sut) //
				.addReference("httpBridgeFactory", factory) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.addReference("oem", new DummyOpenemsEdgeOem() {
					@Override
					public OAuthClientRegistration getRabotChargeCredentials() {
						return new OAuthClientRegistration("dummy_id", "dummy_secret");
					}
				}) //
				.addReference("configurationAdmin", new DummyConfigurationAdmin()) //
				.addReference("authBackend", new DummyOAuthBackend()) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setbackendOAuthClientIdentifier("rabot_prod") //
						.build()) //
				.next(new TestCase() //
						.onBeforeProcessImage(() -> {
							
							// initiateConnect() -> queues getPartnerToken
							var connectFuture = sut.initiateConnect();
							
							// Process token request -> queues createCustomerLink
							executor.update(); 
							
							// Process link request -> safely completes connectFuture
							executor.update(); 
							
							// safely extract the state without deadlocking
							var response = connectFuture.join();
							
							// Simulate OAuth callback completion
							sut.connectCode(response.state(), "75741496");

							executor.update(); // Interceptor calls getPartnerToken()
							executor.update(); // fetches /contracts
							executor.update(); // fetches /costs
							executor.update(); // day-ahead polling timer scheduled
							executor.update(); // fires market prices endpoint
							executor.update(); // parses payload & updates OpenEMS state channel
						})
						.output(new ChannelAddress("ctrl0", "HttpStatusCode"), HttpStatus.OK.code()));
	}

}
