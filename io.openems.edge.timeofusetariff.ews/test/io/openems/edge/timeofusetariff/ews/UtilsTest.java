package io.openems.edge.timeofusetariff.ews;

import static io.openems.edge.timeofusetariff.ews.Data.JSON_DATA;
import static io.openems.edge.timeofusetariff.ews.Utils.parsePrices;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;

public class UtilsTest {

	@Test
	public void nonEmptyStringTest() throws OpenemsNamedException {
		// Parsing with custom data
		var prices = parsePrices(JSON_DATA); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(32.13 * 10, prices.getFirst(), 0.001);

		// To check if a date is present in map.
		assertEquals(ZonedDateTime.of(2025, 9, 1, 0, 0, 0, 0, ZoneId.ofOffset("", ZoneOffset.ofHours(2))).toInstant(),
				prices.getFirstTime());

		// To check 15 minutes values are taken instead of one hour values.
		var firstHour = prices.getFirstTime();
		assertNotNull(prices.getAt(firstHour.plus(15, MINUTES)));
	}

	@Test(expected = OpenemsNamedException.class)
	public void emptyStringTest() throws OpenemsNamedException {
		// Parsing with empty string
		parsePrices("");
	}

	@Test
	public void singleHomeParseTest() throws OpenemsNamedException {
		// Parsing with custom filter. Since the test is single home, the filter should
		// be ignored
		var prices = parsePrices(JSON_DATA); //

		// To check if the Map is not empty
		assertFalse(prices.isEmpty());

		// To check if a value is present in map.
		assertEquals(32.13 * 10, prices.getFirst(), 0.001);
	}
}
