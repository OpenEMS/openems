package io.openems.edge.timeofusetariff.rabotcharge;

import static io.openems.edge.timeofusetariff.rabotcharge.TimeOfUseTariffRabotChargeImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class TimeOfUseTariffRabotChargeImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		var rabotCharge = new TimeOfUseTariffRabotChargeImpl();
		new ComponentTest(rabotCharge) //
				.addReference("httpBridgeFactory", new DummyBridgeHttpFactory()) //
				.addReference("componentManager", cm) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
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
