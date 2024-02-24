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

public class TimeOfUsePricesTest {

	private static final ZonedDateTime TIME = ZonedDateTime.of(2024, 1, 2, 3, 4, 5, 6, ZoneId.of("UTC"));

	@Test
	public void testEmpty() {
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(TIME));
		assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(new TreeMap<>()));
		assertTrue(EMPTY_PRICES.pricePerQuarter.isEmpty());
		assertTrue(EMPTY_PRICES.isEmpty());
		{
			var map = new TreeMap<ZonedDateTime, Double>();
			map.put(TIME, null);
			assertEquals(EMPTY_PRICES, TimeOfUsePrices.from(map));
		}
	}

	@Test
	public void testFromDoubles() {
		var sut = TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3, null, 0.5, null);

		var base = roundDownToQuarter(TIME);
		// Holds 5 keys
		assertArrayEquals(new ZonedDateTime[] { //
				base, //
				base.plusMinutes(15), //
				base.plusMinutes(30), //
				base.plusMinutes(45), //
				base.plusMinutes(60), //
		}, sut.pricePerQuarter.keySet().toArray(ZonedDateTime[]::new));
		// Holds 5 values
		assertArrayEquals(new Double[] { 0.1, 0.2, 0.3, null, 0.5 }, sut.asArray());
	}

	@Test
	public void testFromMap() {
		var base = roundDownToQuarter(TIME);

		var map = new TreeMap<ZonedDateTime, Double>();
		map.put(base, 0.1);
		map.put(base.plusMinutes(1), null);
		map.put(base.plusMinutes(15), 0.2);
		map.put(base.plusMinutes(30), 0.3);
		map.put(base.plusMinutes(60), 0.5);
		map.put(base.plusMinutes(75), null);

		var sut = TimeOfUsePrices.from(map);

		// Holds 5 keys
		assertArrayEquals(new ZonedDateTime[] { //
				base, //
				base.plusMinutes(15), //
				base.plusMinutes(30), //
				base.plusMinutes(45), // added automatically
				base.plusMinutes(60), //
		}, sut.pricePerQuarter.keySet().toArray(ZonedDateTime[]::new));
		// Holds 5 values
		assertArrayEquals(new Double[] { null, 0.2, 0.3, null, 0.5 }, sut.asArray());
	}

	@Test
	public void testFromOther() {
		var other = TimeOfUsePrices.from(TIME, 0.1, 0.2, 0.3, null, 0.5, null);

		// identical
		assertEquals(other, TimeOfUsePrices.from(TIME.plusMinutes(5), other));

		// submap
		assertArrayEquals(new Double[] { 0.2, 0.3, null, 0.5 },
				TimeOfUsePrices.from(TIME.plusMinutes(16), other).asArray());

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
		assertNull(TimeOfUsePrices.from(TIME, null, 0.2, 0.3).getFirst());
		assertNull(TimeOfUsePrices.EMPTY_PRICES.getFirst());
	}
}
