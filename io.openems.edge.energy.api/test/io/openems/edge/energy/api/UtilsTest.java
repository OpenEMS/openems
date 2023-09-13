package io.openems.edge.energy.api;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testToFutureHour() {
		var t = ZonedDateTime.of(2023, 1, 31, 2, 3, 4, 5, ZoneId.systemDefault());
		assertEquals(ZonedDateTime.of(2023, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureHour(t, 0));
		assertEquals(ZonedDateTime.of(2023, 2, 1, 1, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureHour(t, 1));
		assertEquals(ZonedDateTime.of(2023, 1, 31, 2, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureHour(t, 2));
		assertEquals(ZonedDateTime.of(2023, 1, 31, 3, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureHour(t, 3));
	}

	@Test
	public void testToFutureQuarter() {
		var t = ZonedDateTime.of(2023, 1, 31, 2, 3, 4, 5, ZoneId.systemDefault());
		assertEquals(ZonedDateTime.of(2023, 2, 1, 0, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 0));
		assertEquals(ZonedDateTime.of(2023, 2, 1, 0, 15, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 1));
		assertEquals(ZonedDateTime.of(2023, 2, 1, 0, 30, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 2));
		assertEquals(ZonedDateTime.of(2023, 2, 1, 1, 45, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 7));
		assertEquals(ZonedDateTime.of(2023, 1, 31, 2, 0, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 8));
		assertEquals(ZonedDateTime.of(2023, 1, 31, 2, 15, 0, 0, ZoneId.systemDefault()), Utils.toFutureQuarter(t, 9));
	}
}
