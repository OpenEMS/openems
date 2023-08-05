package io.openems.edge.energy.api;

import static org.junit.Assert.assertEquals;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class UtilsTest {

	@Test
	public void testToZonedDateTime() {
		var t = ZonedDateTime.of(2023, 1, 31, 3, 8, 9, 10, ZoneId.systemDefault());
		assertEquals(1, Utils.toZonedDateTime(t, 0).getDayOfMonth());
		assertEquals(1, Utils.toZonedDateTime(t, 1).getDayOfMonth());
		assertEquals(1, Utils.toZonedDateTime(t, 2).getDayOfMonth());
		assertEquals(31, Utils.toZonedDateTime(t, 3).getDayOfMonth());
	}
}
