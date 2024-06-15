package io.openems.edge.timeofusetariff.groupe;

import static io.openems.edge.common.currency.Currency.CHF;
import static io.openems.edge.timeofusetariff.groupe.TimeOfUseTariffGroupeImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.oem.DummyOpenemsEdgeOem;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.bridge.http.dummy.DummyBridgeHttpFactory;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;
import io.openems.edge.common.test.DummyMeta;

public class TimeOfUseTariffGroupeImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final double GROUPE_E_EXCHANGE_RATE = 1;

	private static final String PRICE_RESULT_STRING = """
			[
			  {
			    "start_timestamp": "2024-05-27T10:00:00+02:00",
			    "end_timestamp": "2024-05-27T10:15:00+02:00",
			    "vario_plus": 35.14,
			    "vario_grid": 7.76,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T10:15:00+02:00",
			    "end_timestamp": "2024-05-27T10:30:00+02:00",
			    "vario_plus": 34.55,
			    "vario_grid": 7.21,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T10:30:00+02:00",
			    "end_timestamp": "2024-05-27T10:45:00+02:00",
			    "vario_plus": 34.5,
			    "vario_grid": 7.16,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T10:45:00+02:00",
			    "end_timestamp": "2024-05-27T11:00:00+02:00",
			    "vario_plus": 33.63,
			    "vario_grid": 6.36,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T11:00:00+02:00",
			    "end_timestamp": "2024-05-27T11:15:00+02:00",
			    "vario_plus": 37.13,
			    "vario_grid": 9.59,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T11:15:00+02:00",
			    "end_timestamp": "2024-05-27T11:30:00+02:00",
			    "vario_plus": 34.69,
			    "vario_grid": 7.34,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T11:30:00+02:00",
			    "end_timestamp": "2024-05-27T11:45:00+02:00",
			    "vario_plus": 32.9,
			    "vario_grid": 5.69,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T11:45:00+02:00",
			    "end_timestamp": "2024-05-27T12:00:00+02:00",
			    "vario_plus": 32.16,
			    "vario_grid": 5,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T12:00:00+02:00",
			    "end_timestamp": "2024-05-27T12:15:00+02:00",
			    "vario_plus": 31.97,
			    "vario_grid": 4.82,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T12:15:00+02:00",
			    "end_timestamp": "2024-05-27T12:30:00+02:00",
			    "vario_plus": 33.16,
			    "vario_grid": 5.93,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T12:30:00+02:00",
			    "end_timestamp": "2024-05-27T12:45:00+02:00",
			    "vario_plus": 33.58,
			    "vario_grid": 6.31,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T12:45:00+02:00",
			    "end_timestamp": "2024-05-27T13:00:00+02:00",
			    "vario_plus": 33.76,
			    "vario_grid": 6.48,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T13:00:00+02:00",
			    "end_timestamp": "2024-05-27T13:15:00+02:00",
			    "vario_plus": 32.64,
			    "vario_grid": 5.45,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T13:15:00+02:00",
			    "end_timestamp": "2024-05-27T13:30:00+02:00",
			    "vario_plus": 31.56,
			    "vario_grid": 4.45,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T13:30:00+02:00",
			    "end_timestamp": "2024-05-27T13:45:00+02:00",
			    "vario_plus": 27.05,
			    "vario_grid": 0.27,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T13:45:00+02:00",
			    "end_timestamp": "2024-05-27T14:00:00+02:00",
			    "vario_plus": 26.12,
			    "vario_grid": -0.59,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T14:00:00+02:00",
			    "end_timestamp": "2024-05-27T14:15:00+02:00",
			    "vario_plus": 30.02,
			    "vario_grid": 3.02,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T14:15:00+02:00",
			    "end_timestamp": "2024-05-27T14:30:00+02:00",
			    "vario_plus": 30.44,
			    "vario_grid": 3.41,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T14:30:00+02:00",
			    "end_timestamp": "2024-05-27T14:45:00+02:00",
			    "vario_plus": 28.78,
			    "vario_grid": 1.87,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T14:45:00+02:00",
			    "end_timestamp": "2024-05-27T15:00:00+02:00",
			    "vario_plus": 29.21,
			    "vario_grid": 2.27,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T15:00:00+02:00",
			    "end_timestamp": "2024-05-27T15:15:00+02:00",
			    "vario_plus": 28.92,
			    "vario_grid": 2,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T15:15:00+02:00",
			    "end_timestamp": "2024-05-27T15:30:00+02:00",
			    "vario_plus": 27.82,
			    "vario_grid": 0.99,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T15:30:00+02:00",
			    "end_timestamp": "2024-05-27T15:45:00+02:00",
			    "vario_plus": 28.31,
			    "vario_grid": 1.44,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T15:45:00+02:00",
			    "end_timestamp": "2024-05-27T16:00:00+02:00",
			    "vario_plus": 27.31,
			    "vario_grid": 0.51,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T16:00:00+02:00",
			    "end_timestamp": "2024-05-27T16:15:00+02:00",
			    "vario_plus": 29.52,
			    "vario_grid": 2.56,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T16:15:00+02:00",
			    "end_timestamp": "2024-05-27T16:30:00+02:00",
			    "vario_plus": 28.44,
			    "vario_grid": 1.56,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T16:30:00+02:00",
			    "end_timestamp": "2024-05-27T16:45:00+02:00",
			    "vario_plus": 30.36,
			    "vario_grid": 3.33,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T16:45:00+02:00",
			    "end_timestamp": "2024-05-27T17:00:00+02:00",
			    "vario_plus": 31.66,
			    "vario_grid": 4.54,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T17:00:00+02:00",
			    "end_timestamp": "2024-05-27T17:15:00+02:00",
			    "vario_plus": 30.53,
			    "vario_grid": 3.5,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T17:15:00+02:00",
			    "end_timestamp": "2024-05-27T17:30:00+02:00",
			    "vario_plus": 31.47,
			    "vario_grid": 4.36,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T17:30:00+02:00",
			    "end_timestamp": "2024-05-27T17:45:00+02:00",
			    "vario_plus": 32.13,
			    "vario_grid": 4.97,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T17:45:00+02:00",
			    "end_timestamp": "2024-05-27T18:00:00+02:00",
			    "vario_plus": 32.74,
			    "vario_grid": 5.54,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T18:00:00+02:00",
			    "end_timestamp": "2024-05-27T18:15:00+02:00",
			    "vario_plus": 32.98,
			    "vario_grid": 5.76,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T18:15:00+02:00",
			    "end_timestamp": "2024-05-27T18:30:00+02:00",
			    "vario_plus": 34.31,
			    "vario_grid": 6.98,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T18:30:00+02:00",
			    "end_timestamp": "2024-05-27T18:45:00+02:00",
			    "vario_plus": 35.51,
			    "vario_grid": 8.1,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T18:45:00+02:00",
			    "end_timestamp": "2024-05-27T19:00:00+02:00",
			    "vario_plus": 35.85,
			    "vario_grid": 8.42,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T19:00:00+02:00",
			    "end_timestamp": "2024-05-27T19:15:00+02:00",
			    "vario_plus": 37.84,
			    "vario_grid": 10.25,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T19:15:00+02:00",
			    "end_timestamp": "2024-05-27T19:30:00+02:00",
			    "vario_plus": 40.16,
			    "vario_grid": 12.4,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T19:30:00+02:00",
			    "end_timestamp": "2024-05-27T19:45:00+02:00",
			    "vario_plus": 42.88,
			    "vario_grid": 14.92,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T19:45:00+02:00",
			    "end_timestamp": "2024-05-27T20:00:00+02:00",
			    "vario_plus": 46.44,
			    "vario_grid": 18.21,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T20:00:00+02:00",
			    "end_timestamp": "2024-05-27T20:15:00+02:00",
			    "vario_plus": 40.79,
			    "vario_grid": 12.98,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T20:15:00+02:00",
			    "end_timestamp": "2024-05-27T20:30:00+02:00",
			    "vario_plus": 41.56,
			    "vario_grid": 13.7,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T20:30:00+02:00",
			    "end_timestamp": "2024-05-27T20:45:00+02:00",
			    "vario_plus": 41.82,
			    "vario_grid": 13.93,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T20:45:00+02:00",
			    "end_timestamp": "2024-05-27T21:00:00+02:00",
			    "vario_plus": 41.74,
			    "vario_grid": 13.86,
			    "dt_plus": 35.44,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T21:00:00+02:00",
			    "end_timestamp": "2024-05-27T21:15:00+02:00",
			    "vario_plus": 34.36,
			    "vario_grid": 11.34,
			    "dt_plus": 25.08,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T21:15:00+02:00",
			    "end_timestamp": "2024-05-27T21:30:00+02:00",
			    "vario_plus": 34.06,
			    "vario_grid": 11.06,
			    "dt_plus": 25.08,
			    "unit": "Rp./kWh"
			  },
			  {
			    "start_timestamp": "2024-05-27T21:30:00+02:00",
			    "end_timestamp": "2024-05-27T21:45:00+02:00",
			    "vario_plus": 35.8,
			    "vario_grid": 12.67,
			    "dt_plus": 25.08,
			    "unit": "Rp./kWh"
			  }
			]
			""";

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		var groupe = new TimeOfUseTariffGroupeImpl();
		var dummyMeta = new DummyMeta("foo0") //
				.withCurrency(CHF);
		new ComponentTest(groupe) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("meta", dummyMeta) //
				.addReference("oem", new DummyOpenemsEdgeOem()) //
				.addReference("componentManager", cm) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setExchangerateAccesskey("") //
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
		assertEquals(351.4, prices.getFirst(), 0.001);
	}

	@Test
	public void emptyStringTest() throws OpenemsNamedException {
		assertThrows(OpenemsNamedException.class, () -> {
			parsePrices("", GROUPE_E_EXCHANGE_RATE);
		});
	}
}
