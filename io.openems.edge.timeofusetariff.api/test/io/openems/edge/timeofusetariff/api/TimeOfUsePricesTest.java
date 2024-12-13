package io.openems.edge.timeofusetariff.api;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.timeofusetariff.api.TimeOfUsePrices.EMPTY_PRICES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

public class TimeOfUsePricesTest {

	private static final ZonedDateTime TIME = ZonedDateTime.of(2024, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"));

	@Test
	public void testEmpty() {
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(TIME));
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(ImmutableSortedMap.of()));
		assertTrue(EMPTY_PRICES.isEmpty());
	}

	@Test
	public void testFromDoubles() {
		var sut = TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3, null, 0.5, null);

		// Holds 4 values
		assertArrayEquals(new Double[] { 0.1, 0.2, 0.3, 0.5 }, sut.asArray());
	}

	@Test(expected = NullPointerException.class)
	public void testValidateQuarterKeys() {
		var base = roundDownToQuarter(TIME);
		TimeOfUsePrices.from(ImmutableSortedMap.of(//
				base, 0.1, //
				base.plusMinutes(1), null, //
				base.plusMinutes(15), 0.2, //
				base.plusMinutes(30), 0.3, //
				base.plusMinutes(60), 0.5, //
				base.plusMinutes(75), null));
	}

	@Test
	public void testFromMap() {
		var base = roundDownToQuarter(TIME);
		var map = ImmutableSortedMap.of(//
				base, 0.1, //
				base.plusMinutes(15), 0.2, //
				base.plusMinutes(30), 0.3, //
				base.plusMinutes(60), 0.5);

		var sut = TimeOfUsePrices.from(map);

		// Holds 4 native keys
		assertEquals(4, sut.asArray().length);

		var x = new TreeMap<ZonedDateTime, Integer>();
		x.put(base, null);

		// Fills up to 5 keys
		assertEquals(5, sut.toMapWithAllQuarters().size());
	}

	@Test
	public void testFromOther() {
		var other = TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3, null, 0.5, null);

		// identical
		assertEquals(other, TimeOfUsePrices.from(TIME.plusMinutes(5), other));

		// submap
		assertArrayEquals(new Double[] { 0.2, 0.3, 0.5 }, TimeOfUsePrices.from(TIME.plusMinutes(16), other).asArray());

		// empty
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(TIME.plusDays(1), other));

		// corner cases
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(TIME, EMPTY_PRICES));
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(null, EMPTY_PRICES));
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(TIME, (TimeOfUsePrices) null));
	}

	@Test
	public void testGetFirst() {
		assertEquals(0.1, TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3).getFirst(), 0.001);
		assertEquals(0.2, TimeOfUsePrices.from(TIME, null, 0.2, 0.3).getFirst(), 0.001);
		assertNull(TimeOfUsePrices.EMPTY_PRICES.getFirst());
	}

	@Test
	public void testGet() {
		final var sut = TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3);
		var base = roundDownToQuarter(TIME);
		assertEquals(0.1, sut.getAt(base), 0.001);
		assertEquals(0.1, sut.getAt(base.withZoneSameInstant(ZoneId.of("Europe/Berlin"))), 0.001);
	}
}
