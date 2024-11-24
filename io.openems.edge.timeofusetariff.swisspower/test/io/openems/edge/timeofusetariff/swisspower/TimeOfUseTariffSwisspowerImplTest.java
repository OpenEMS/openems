package io.openems.edge.timeofusetariff.swisspower;

import static io.openems.edge.common.currency.Currency.EUR;
import static io.openems.edge.common.test.TestUtils.createDummyClock;
import static io.openems.edge.timeofusetariff.swisspower.TimeOfUseTariffSwisspowerImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

public class TimeOfUseTariffSwisspowerImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final double GROUPE_E_EXCHANGE_RATE = 1;

	private static final String PRICE_RESULT_STRING = """
							{
							  "status": "ok",
							  "prices": [
							    {
							      "start_timestamp": "2024-08-12T00:00:00+02:00",
							      "end_timestamp": "2024-08-12T00:15:00+02:00",
							      "integrated": [
							        {
							          "value": 0.49249999999999994,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T00:15:00+02:00",
							      "end_timestamp": "2024-08-12T00:30:00+02:00",
							      "integrated": [
							        {
							          "value": 0.491133,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T00:30:00+02:00",
							      "end_timestamp": "2024-08-12T00:45:00+02:00",
							      "integrated": [
							        {
							          "value": 0.486722,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T00:45:00+02:00",
							      "end_timestamp": "2024-08-12T01:00:00+02:00",
							      "integrated": [
							        {
							          "value": 0.478854,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T01:00:00+02:00",
							      "end_timestamp": "2024-08-12T01:15:00+02:00",
							      "integrated": [
							        {
							          "value": 0.46720300000000003,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T01:15:00+02:00",
							      "end_timestamp": "2024-08-12T01:30:00+02:00",
							      "integrated": [
							        {
							          "value": 0.451539,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T01:30:00+02:00",
							      "end_timestamp": "2024-08-12T01:45:00+02:00",
							      "integrated": [
							        {
							          "value": 0.431753,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T01:45:00+02:00",
							      "end_timestamp": "2024-08-12T02:00:00+02:00",
							      "integrated": [
							        {
							          "value": 0.407858,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T02:00:00+02:00",
							      "end_timestamp": "2024-08-12T02:15:00+02:00",
							      "integrated": [
							        {
							          "value": 0.38,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T02:15:00+02:00",
							      "end_timestamp": "2024-08-12T02:30:00+02:00",
							      "integrated": [
							        {
							          "value": 0.348458,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T02:30:00+02:00",
							      "end_timestamp": "2024-08-12T02:45:00+02:00",
							      "integrated": [
							        {
							          "value": 0.3425,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T02:45:00+02:00",
							      "end_timestamp": "2024-08-12T03:00:00+02:00",
							      "integrated": [
							        {
							          "value": 0.3425,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T03:00:00+02:00",
							      "end_timestamp": "2024-08-12T03:15:00+02:00",
							      "integrated": [
							        {
							          "value": 0.3425,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T03:15:00+02:00",
							      "end_timestamp": "2024-08-12T03:30:00+02:00",
							      "integrated": [
							        {
							          "value": 0.3425,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    },
							    {
							      "start_timestamp": "2024-08-12T03:30:00+02:00",
							      "end_timestamp": "2024-08-12T03:45:00+02:00",
							      "integrated": [
							        {
							          "value": 0.3425,
							          "unit": "CHF/kWh",
							          "component": "work"
							        }
							      ]
							    }
							  ]
							}

			""";

	@Test
	public void test() throws Exception {
		final var clock = createDummyClock();
		var swissPower = new TimeOfUseTariffSwisspowerImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(EUR);
		new ComponentTest(swissPower) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("meta", dummyMeta) //
				.addReference("componentManager", new DummyComponentManager(clock)) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAccessToken("foo-bar") //
						.setMeteringCode("") //
						.build()) //
		;
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(PRICE_RESULT_STRING, GROUPE_E_EXCHANGE_RATE); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(492.499, prices.getFirst(), 0.001);
	}

}
