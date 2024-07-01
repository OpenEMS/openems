package io.openems.edge.timeofusetariff.corrently;

import static io.openems.edge.timeofusetariff.corrently.TimeOfUseTariffCorrentlyImpl.parsePrices;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.ComponentTest;

public class TimeOfUseTariffCorrentlyImplTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		var corrently = new TimeOfUseTariffCorrentlyImpl();
		new ComponentTest(corrently) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setZipcode("94469" /* Deggendorf, Germany */) //
						.build()) //
		;

		// Thread.sleep(5000);
		// System.out.println(sut.getPrices());
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices("""
								{
				   "object":"list",
				   "data":[
				      {
				         "start_timestamp":1632402000000,
				         "end_timestamp":1632405600000,
				         "marketprice":158.95,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632405600000,
				         "end_timestamp":1632409200000,
				         "marketprice":160.98,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632409200000,
				         "end_timestamp":1632412800000,
				         "marketprice":171.15,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632412800000,
				         "end_timestamp":1632416400000,
				         "marketprice":174.96,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632416400000,
				         "end_timestamp":1632420000000,
				         "marketprice":161.53,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632420000000,
				         "end_timestamp":1632423600000,
				         "marketprice":152,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632423600000,
				         "end_timestamp":1632427200000,
				         "marketprice":120.01,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632427200000,
				         "end_timestamp":1632430800000,
				         "marketprice":111.03,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632430800000,
				         "end_timestamp":1632434400000,
				         "marketprice":105.04,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632434400000,
				         "end_timestamp":1632438000000,
				         "marketprice":105,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632438000000,
				         "end_timestamp":1632441600000,
				         "marketprice":74.23,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632441600000,
				         "end_timestamp":1632445200000,
				         "marketprice":73.28,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632445200000,
				         "end_timestamp":1632448800000,
				         "marketprice":67.97,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632448800000,
				         "end_timestamp":1632452400000,
				         "marketprice":72.53,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632452400000,
				         "end_timestamp":1632456000000,
				         "marketprice":89.66,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632456000000,
				         "end_timestamp":1632459600000,
				         "marketprice":150.1,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632459600000,
				         "end_timestamp":1632463200000,
				         "marketprice":173.54,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632463200000,
				         "end_timestamp":1632466800000,
				         "marketprice":178.4,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632466800000,
				         "end_timestamp":1632470400000,
				         "marketprice":158.91,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632470400000,
				         "end_timestamp":1632474000000,
				         "marketprice":140.01,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632474000000,
				         "end_timestamp":1632477600000,
				         "marketprice":149.99,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632477600000,
				         "end_timestamp":1632481200000,
				         "marketprice":157.43,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632481200000,
				         "end_timestamp":1632484800000,
				         "marketprice":130.9,
				         "unit":"Eur/MWh"
				      },
				      {
				         "start_timestamp":1632484800000,
				         "end_timestamp":1632488400000,
				         "marketprice":120.14,
				         "unit":"Eur/MWh"
				      }
				   ],
				   "url":"/at/v1/marketdata"
				}"""); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(158.95, prices.getFirst(), 0.001);
	}

	@Test
	public void emptyStringTest() throws OpenemsNamedException {
		try {
			// Parsing with empty string
			parsePrices("");
		} catch (OpenemsNamedException e) {
			// expected
			return;
		}

		fail("Expected Exception");
	}
}
