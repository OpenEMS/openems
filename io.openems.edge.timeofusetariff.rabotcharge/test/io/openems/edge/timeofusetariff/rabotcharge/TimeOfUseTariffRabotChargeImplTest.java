package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.timeofusetariff.rabotcharge.TimeOfUseTariffRabotChargeImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TimeOfUseTariffRabotChargeImplTest {

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		new ComponentTest(new TimeOfUseTariffRabotChargeImpl()) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId("ctrl0") //
						.setAccessToken("foo-bar") //
						.build()) //
		;
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices("""
				{
				    "records": [
				        {
				            "moment": "2024-05-14T09:00:00+02:00",
				            "price_inCentPerKwh": 0.564
				        },
				        {
				            "moment": "2024-05-14T10:00:00+02:00",
				            "price_inCentPerKwh": -0.233
				        },
				        {
				            "moment": "2024-05-14T11:00:00+02:00",
				            "price_inCentPerKwh": -1.112
				        },
				        {
				            "moment": "2024-05-14T12:00:00+02:00",
				            "price_inCentPerKwh": -3.633
				        },
				        {
				            "moment": "2024-05-14T13:00:00+02:00",
				            "price_inCentPerKwh": -4.295
				        },
				        {
				            "moment": "2024-05-14T14:00:00+02:00",
				            "price_inCentPerKwh": -3.966
				        },
				        {
				            "moment": "2024-05-14T15:00:00+02:00",
				            "price_inCentPerKwh": -1.986
				        },
				        {
				            "moment": "2024-05-14T16:00:00+02:00",
				            "price_inCentPerKwh": -0.293
				        },
				        {
				            "moment": "2024-05-14T17:00:00+02:00",
				            "price_inCentPerKwh": -0.004
				        },
				        {
				            "moment": "2024-05-14T18:00:00+02:00",
				            "price_inCentPerKwh": 4.374
				        },
				        {
				            "moment": "2024-05-14T19:00:00+02:00",
				            "price_inCentPerKwh": 8.390
				        },
				        {
				            "moment": "2024-05-14T20:00:00+02:00",
				            "price_inCentPerKwh": 9.347
				        },
				        {
				            "moment": "2024-05-14T21:00:00+02:00",
				            "price_inCentPerKwh": 7.641
				        },
				        {
				            "moment": "2024-05-14T22:00:00+02:00",
				            "price_inCentPerKwh": 4.535
				        },
				        {
				            "moment": "2024-05-14T23:00:00+02:00",
				            "price_inCentPerKwh": 2.771
				        }
				    ],
				    "success": true,
				    "metadata": {
				        "messages": [],
				        "maintenanceMode": null
				    }

				}"""); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(5.64, prices.getFirst(), 0.001);
	}

	@Test
	public void emptyStringTest() throws OpenemsNamedException {
		assertThrows(OpenemsNamedException.class, () -> {
			parsePrices("");
		});
	}
}
