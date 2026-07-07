package io.openems.edge.timeofusetariff.luox;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.time.ZonedDateTime;

import org.junit.Test;

public class LuoxApiTest {

	private static final String EXAMPLE_RESPONSE = """
			[
			    {
			        "valid_from": "2025-08-28T02:00:00+02:00",
			        "valid_until": "2025-08-29T00:00:00+02:00",
			        "fixed_price": {
			            "total_price_net": "1078.1513",
			            "unit": "ct/m"
			        },
			        "variable_prices": [
			            {
			                "valid_from": "2025-08-28T02:00:00+02:00",
			                "valid_until": "2025-08-28T03:00:00+02:00",
			                "energy_price_net": "-8.7890",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6166",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-21.8766"
			            },
			            {
			                "valid_from": "2025-08-28T03:00:00+02:00",
			                "valid_until": "2025-08-28T04:00:00+02:00",
			                "energy_price_net": "-8.7090",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6142",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-21.7942"
			            },
			            {
			                "valid_from": "2025-08-28T04:00:00+02:00",
			                "valid_until": "2025-08-28T05:00:00+02:00",
			                "energy_price_net": "-9.0230",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6236",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-22.1176"
			            },
			            {
			                "valid_from": "2025-08-28T05:00:00+02:00",
			                "valid_until": "2025-08-28T06:00:00+02:00",
			                "energy_price_net": "-9.8720",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6491",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-22.9921"
			            },
			            {
			                "valid_from": "2025-08-28T06:00:00+02:00",
			                "valid_until": "2025-08-28T07:00:00+02:00",
			                "energy_price_net": "-12.1630",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7178",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-25.3518"
			            },
			            {
			                "valid_from": "2025-08-28T07:00:00+02:00",
			                "valid_until": "2025-08-28T08:00:00+02:00",
			                "energy_price_net": "-14.3790",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7843",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-27.6343"
			            },
			            {
			                "valid_from": "2025-08-28T08:00:00+02:00",
			                "valid_until": "2025-08-28T09:00:00+02:00",
			                "energy_price_net": "-14.3950",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7848",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-27.6508"
			            },
			            {
			                "valid_from": "2025-08-28T09:00:00+02:00",
			                "valid_until": "2025-08-28T10:00:00+02:00",
			                "energy_price_net": "-12.1840",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7185",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-25.3735"
			            },
			            {
			                "valid_from": "2025-08-28T10:00:00+02:00",
			                "valid_until": "2025-08-28T11:00:00+02:00",
			                "energy_price_net": "-11.2270",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6898",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-24.3878"
			            },
			            {
			                "valid_from": "2025-08-28T11:00:00+02:00",
			                "valid_until": "2025-08-28T12:00:00+02:00",
			                "energy_price_net": "-10.8130",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6773",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.9613"
			            },
			            {
			                "valid_from": "2025-08-28T12:00:00+02:00",
			                "valid_until": "2025-08-28T13:00:00+02:00",
			                "energy_price_net": "-10.0890",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6556",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.2156"
			            },
			            {
			                "valid_from": "2025-08-28T13:00:00+02:00",
			                "valid_until": "2025-08-28T14:00:00+02:00",
			                "energy_price_net": "-10.0960",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6558",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.2228"
			            },
			            {
			                "valid_from": "2025-08-28T14:00:00+02:00",
			                "valid_until": "2025-08-28T15:00:00+02:00",
			                "energy_price_net": "-10.0890",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6556",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.2156"
			            },
			            {
			                "valid_from": "2025-08-28T15:00:00+02:00",
			                "valid_until": "2025-08-28T16:00:00+02:00",
			                "energy_price_net": "-10.0660",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6549",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.1919"
			            },
			            {
			                "valid_from": "2025-08-28T16:00:00+02:00",
			                "valid_until": "2025-08-28T17:00:00+02:00",
			                "energy_price_net": "-9.6100",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6412",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-22.7222"
			            },
			            {
			                "valid_from": "2025-08-28T17:00:00+02:00",
			                "valid_until": "2025-08-28T18:00:00+02:00",
			                "energy_price_net": "-12.9840",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7425",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-26.1975"
			            },
			            {
			                "valid_from": "2025-08-28T18:00:00+02:00",
			                "valid_until": "2025-08-28T19:00:00+02:00",
			                "energy_price_net": "-16.0850",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.8355",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-29.3915"
			            },
			            {
			                "valid_from": "2025-08-28T19:00:00+02:00",
			                "valid_until": "2025-08-28T20:00:00+02:00",
			                "energy_price_net": "-21.0000",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.9829",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-34.4539"
			            },
			            {
			                "valid_from": "2025-08-28T20:00:00+02:00",
			                "valid_until": "2025-08-28T21:00:00+02:00",
			                "energy_price_net": "-15.3160",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.8124",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-28.5994"
			            },
			            {
			                "valid_from": "2025-08-28T21:00:00+02:00",
			                "valid_until": "2025-08-28T22:00:00+02:00",
			                "energy_price_net": "-12.4180",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.7255",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-25.6145"
			            },
			            {
			                "valid_from": "2025-08-28T22:00:00+02:00",
			                "valid_until": "2025-08-28T23:00:00+02:00",
			                "energy_price_net": "-10.7110",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6743",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-23.8563"
			            },
			            {
			                "valid_from": "2025-08-28T23:00:00+02:00",
			                "valid_until": "2025-08-29T00:00:00+02:00",
			                "energy_price_net": "-9.2150",
			                "taxes_and_levies_net": "-6.0210",
			                "fees_net": "-0.6294",
			                "grid_costs_net": "-6.4500",
			                "unit": "ct/kWh",
			                "total_price_net": "-22.3154"
			            }
			        ]
			    }
			]
			""";

	@Test
	public void testParse() throws Exception {
		final var parsedResponse = LuoxApi.PricesResponse.serializer().deserialize(EXAMPLE_RESPONSE);
		assertNotNull(parsedResponse);
		var sut = parsedResponse.toTimeOfUsePrices();
		assertEquals(88, sut.asArray().length);
		assertEquals(260.332 /* net:218.766 */, //
				sut.getAt(ZonedDateTime.parse("2025-08-28T02:00+02:00")).doubleValue(), 0.001);
		assertEquals(260.332 /* net:218.766 */, //
				sut.getAt(ZonedDateTime.parse("2025-08-28T02:15+02:00")).doubleValue(), 0.001);
		assertEquals(259.351 /* net:217.942 */, //
				sut.getAt(ZonedDateTime.parse("2025-08-28T03:00+02:00")).doubleValue(), 0.001);
		assertEquals(263.200 /* net:221.176 */, //
				sut.getAt(ZonedDateTime.parse("2025-08-28T04:00+02:00")).doubleValue(), 0.001);
		assertEquals(265.553 /* net:223.154 */, //
				sut.getAt(ZonedDateTime.parse("2025-08-28T23:45+02:00")).doubleValue(), 0.001);
		assertNull(sut.getAt(ZonedDateTime.parse("2025-08-29T00:00+02:00")));
	}
}