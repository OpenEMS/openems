package io.openems.edge.timeofusetariff.tibber;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;
import java.util.SortedMap;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.test.ComponentTest;

public class TibberProviderTest {

	private static final String CTRL_ID = "ctrl0";

	@Test
	public void test() throws Exception {
		var sut = new TibberImpl();
		new ComponentTest(sut) //
				.activate(MyConfig.create() //
						.setId(CTRL_ID) //
						.setAccessToken("dummy") //
						.build()) //
		;

		// Thread.sleep(5000);
		// System.out.println(sut.getPrices());
	}

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		SortedMap<ZonedDateTime, Float> prices = TibberImpl.parsePrices("{\n" //
				+ "  \"data\": {\n" + "    \"viewer\": {\n" //
				+ "      \"homes\": [\n" //
				+ "        {\n" //
				+ "          \"currentSubscription\": {\n" //
				+ "            \"priceInfo\": {\n" //
				+ "              \"today\": [\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.187,\n"
				+ "                  \"startsAt\": \"2021-11-15T00:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1841,\n"
				+ "                  \"startsAt\": \"2021-11-15T01:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.18,\n"
				+ "                  \"startsAt\": \"2021-11-15T02:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1747,\n"
				+ "                  \"startsAt\": \"2021-11-15T03:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.179,\n"
				+ "                  \"startsAt\": \"2021-11-15T04:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1809,\n"
				+ "                  \"startsAt\": \"2021-11-15T05:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1795,\n"
				+ "                  \"startsAt\": \"2021-11-15T06:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1848,\n"
				+ "                  \"startsAt\": \"2021-11-15T07:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1868,\n"
				+ "                  \"startsAt\": \"2021-11-15T08:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1876,\n"
				+ "                  \"startsAt\": \"2021-11-15T09:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1874,\n"
				+ "                  \"startsAt\": \"2021-11-15T10:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1874,\n"
				+ "                  \"startsAt\": \"2021-11-15T11:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1875,\n"
				+ "                  \"startsAt\": \"2021-11-15T12:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1878,\n"
				+ "                  \"startsAt\": \"2021-11-15T13:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1868,\n"
				+ "                  \"startsAt\": \"2021-11-15T14:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1853,\n"
				+ "                  \"startsAt\": \"2021-11-15T15:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1848,\n"
				+ "                  \"startsAt\": \"2021-11-15T16:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1829,\n"
				+ "                  \"startsAt\": \"2021-11-15T17:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1835,\n"
				+ "                  \"startsAt\": \"2021-11-15T18:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1846,\n"
				+ "                  \"startsAt\": \"2021-11-15T19:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1851,\n"
				+ "                  \"startsAt\": \"2021-11-15T20:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1854,\n"
				+ "                  \"startsAt\": \"2021-11-15T21:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1853,\n"
				+ "                  \"startsAt\": \"2021-11-15T22:00:00.000+01:00\"\n" //
				+ "                },\n" //
				+ "                {\n" //
				+ "                  \"total\": 0.1831,\n"
				+ "                  \"startsAt\": \"2021-11-15T23:00:00.000+01:00\"\n" //
				+ "                }\n" //
				+ "              ],\n" //
				+ "              \"tomorrow\": []\n" //
				+ "            }\n" //
				+ "          }\n" //
				+ "        }\n" //
				+ "      ]\n" //
				+ "    }\n" //
				+ "  }\n" //
				+ "}"); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if the a value input from the string is present in map.
		assertTrue(prices.containsValue(0.1853f * 1000));

	}

	@Test
	public void emptyStringTest() throws OpenemsNamedException {
		// Parsing with empty string
		SortedMap<ZonedDateTime, Float> prices = TibberImpl.parsePrices("");

		// To check if the map is empty.
		assertTrue(prices.isEmpty());
	}
}
