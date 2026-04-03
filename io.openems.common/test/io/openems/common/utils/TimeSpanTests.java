package io.openems.common.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Test;

public class TimeSpanTests {
	@Test
	public void testTimeSpan() {
		final var span1 = TimeSpan.between(Instant.parse("2026-02-03T12:00:00Z"),
				Instant.parse("2026-02-03T18:00:00Z"));
		final var span2 = TimeSpan.between(Instant.parse("2026-02-03T13:00:00Z"),
				Instant.parse("2026-02-03T13:15:00Z"));
		final var span3 = TimeSpan.between(Instant.parse("2026-02-03T17:00:00Z"),
				Instant.parse("2026-02-03T18:00:00Z"));
		final var span4 = TimeSpan.between(Instant.parse("2026-02-03T18:00:00Z"),
				Instant.parse("2026-02-03T18:15:00Z"));
		final var span5 = TimeSpan.between(Instant.parse("2026-02-03T20:00:00Z"),
				Instant.parse("2026-02-03T21:00:00Z"));
		final var span6 = TimeSpan.between(Instant.parse("2026-02-03T12:00:00Z"),
				Instant.parse("2026-02-03T12:00:01Z"));

		assertFalse(span1.getEndExclusive().isAfter(span4.getStartInclusive()));

		assertTrue(span1.overlapsWith(span2));
		assertTrue(span2.overlapsWith(span1));
		assertTrue(span3.overlapsWith(span1));
		assertFalse(span4.overlapsWith(span1));
		assertFalse(span5.overlapsWith(span1));
		assertFalse(span2.overlapsWith(span3));
		assertTrue(span6.overlapsWith(span1));

		assertTrue(span2.isContainedIn(span1));
		assertFalse(span1.isContainedIn(span2));
	}
}
