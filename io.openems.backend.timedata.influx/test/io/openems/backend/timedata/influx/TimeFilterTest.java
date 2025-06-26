package io.openems.backend.timedata.influx;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.Test;

public class TimeFilterTest {

	// Sunday, 1. January 2023 00:00:00
	private static final long T_01_01_2023 = 1672531200000L;

	private static final ZonedDateTime Z_01_01_1970 = ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));
	private static final ZonedDateTime Z_01_01_2023 = ZonedDateTime.of(2023, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"));

	@Test
	public void testTimestamp() {
		assertTrue(TimeFilter.from(null, null).isValid(0));

		assertFalse(TimeFilter.from("2000-01-30", null).isValid(0));
		assertTrue(TimeFilter.from("2000-01-30", null).isValid(T_01_01_2023));

		assertFalse(TimeFilter.from(null, "2000-01-30").isValid(T_01_01_2023));
		assertTrue(TimeFilter.from(null, "2024-01-30").isValid(T_01_01_2023));
	}

	@Test
	public void testZonedDateTime() {
		assertTrue(TimeFilter.from(null, null).isValid(Z_01_01_1970, Z_01_01_2023));

		assertFalse(TimeFilter.from("2000-01-30", null).isValid(Z_01_01_1970));
		assertTrue(TimeFilter.from("2000-01-30", null).isValid(Z_01_01_2023));

		assertFalse(TimeFilter.from(null, "2000-01-30").isValid(Z_01_01_2023));
		assertTrue(TimeFilter.from(null, "2024-01-30").isValid(Z_01_01_2023));
	}

}
