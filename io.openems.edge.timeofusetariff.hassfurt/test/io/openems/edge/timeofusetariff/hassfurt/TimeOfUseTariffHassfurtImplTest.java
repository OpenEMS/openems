package io.openems.edge.timeofusetariff.hassfurt;

import static io.openems.edge.timeofusetariff.hassfurt.TimeOfUseTariffHassfurtImpl.parsePrices;
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

public class TimeOfUseTariffHassfurtImplTest {

	private static final String CTRL_ID = "ctrl0";
	private static final String STROM_FLEX_PRO_STRING = """
				{
				    "object": "list",
				    "tariff_info_flex_pro": {
				        "name": "haStrom FLEX PRO",
				        "minimal_energy_price": "5.95 ct/kWh",
				        "taxes": "5.59 ct/kWh",
				        "netcosts": "8.57 ct/kWh",
				        "margin": "2.97 ct/kWh",
				        "basic_charge": "210.0 EUR/year",
				        "vat": "19 %"
				    },
				    "data": [
				        {
				            "start_timestamp": "2024-05-22 00:00:00",
				            "end_timestamp": "2024-05-22 01:00:00",
				            "e_price_epex_excl_vat": 7.099,
				            "e_price_has_pro_incl_vat": 11.423,
				            "t_price_has_pro_incl_vat": 25.58,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 01:00:00",
				            "end_timestamp": "2024-05-22 02:00:00",
				            "e_price_epex_excl_vat": 6.675,
				            "e_price_has_pro_incl_vat": 10.918,
				            "t_price_has_pro_incl_vat": 25.08,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 02:00:00",
				            "end_timestamp": "2024-05-22 03:00:00",
				            "e_price_epex_excl_vat": 6.471,
				            "e_price_has_pro_incl_vat": 10.675,
				            "t_price_has_pro_incl_vat": 24.84,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 03:00:00",
				            "end_timestamp": "2024-05-22 04:00:00",
				            "e_price_epex_excl_vat": 5.428,
				            "e_price_has_pro_incl_vat": 9.434,
				            "t_price_has_pro_incl_vat": 23.6,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 04:00:00",
				            "end_timestamp": "2024-05-22 05:00:00",
				            "e_price_epex_excl_vat": 6.228,
				            "e_price_has_pro_incl_vat": 10.386,
				            "t_price_has_pro_incl_vat": 24.55,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 05:00:00",
				            "end_timestamp": "2024-05-22 06:00:00",
				            "e_price_epex_excl_vat": 7.029,
				            "e_price_has_pro_incl_vat": 11.34,
				            "t_price_has_pro_incl_vat": 25.5,
				            "unit": "ct/kWh"
				        },
				        {
				            "start_timestamp": "2024-05-22 06:00:00",
				            "end_timestamp": "2024-05-22 07:00:00",
				            "e_price_epex_excl_vat": 9.726,
				            "e_price_has_pro_incl_vat": 14.549,
				            "t_price_has_pro_incl_vat": 28.71,
				            "unit": "ct/kWh"
				        }
				    ]
				}
			""";

	private static final String STROM_FLEX_STRING = """
					{
					    "object": "list",
					    "tariff_info": {
					        "name": "haStrom flex",
					        "minimal_energy_price": "17.37 ct/kWh",
					        "maximal_energy_price": "28.5 ct/kWh",
					        "taxes": "5.59 ct/kWh",
					        "netcosts": "8.57 ct/kWh",
					        "margin": "2.97 ct/kWh",
					        "basic_charge": "210.0 EUR/year",
					        "vat": "19 %"
					    },
					    "data": [
					        {
					            "start_timestamp": "2024-05-20 00:00:00",
					            "end_timestamp": "2024-05-20 01:00:00",
					            "e_price_epex_excl_vat": 9.1,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 01:00:00",
					            "end_timestamp": "2024-05-20 02:00:00",
					            "e_price_epex_excl_vat": 8.296,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 02:00:00",
					            "end_timestamp": "2024-05-20 03:00:00",
					            "e_price_epex_excl_vat": 8.619,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 03:00:00",
					            "end_timestamp": "2024-05-20 04:00:00",
					            "e_price_epex_excl_vat": 8.558,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 04:00:00",
					            "end_timestamp": "2024-05-20 05:00:00",
					            "e_price_epex_excl_vat": 8.769,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 05:00:00",
					            "end_timestamp": "2024-05-20 06:00:00",
					            "e_price_epex_excl_vat": 9.061,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 06:00:00",
					            "end_timestamp": "2024-05-20 07:00:00",
					            "e_price_epex_excl_vat": 10.348,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 07:00:00",
					            "end_timestamp": "2024-05-20 08:00:00",
					            "e_price_epex_excl_vat": 10.41,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 08:00:00",
					            "end_timestamp": "2024-05-20 09:00:00",
					            "e_price_epex_excl_vat": 7.19,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        },
					        {
					            "start_timestamp": "2024-05-20 09:00:00",
					            "end_timestamp": "2024-05-20 10:00:00",
					            "e_price_epex_excl_vat": 3.289,
					            "e_price_has_incl_vat": 17.374,
					            "t_price_has_incl_vat": 31.54,
					            "unit": "ct/kWh"
					        }
					    ]
					}
			""";

	@Test
	public void test() throws Exception {
		final TimeLeapClock clock = new TimeLeapClock(Instant.parse("2020-01-01T01:00:00.00Z"), ZoneOffset.UTC);
		final DummyComponentManager cm = new DummyComponentManager(clock);
		var hassfurt = new TimeOfUseTariffHassfurtImpl();
		new ComponentTest(hassfurt) //
				.addReference("httpBridgeFactory", DummyBridgeHttpFactory.ofDummyBridge()) //
				.addReference("componentManager", cm) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setTariffType(TariffType.STROM_FLEX) //
						.build()) //
		;
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(STROM_FLEX_STRING, TariffType.STROM_FLEX); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(315.4, prices.getFirst(), 0.001);

		prices = parsePrices(STROM_FLEX_PRO_STRING, TariffType.STROM_FLEX_PRO); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(255.8, prices.getFirst(), 0.001);
	}

	@Test
	public void emptyStringTest() {
		assertThrows(OpenemsNamedException.class, () -> {
			parsePrices("", TariffType.STROM_FLEX);
		});
	}
}
