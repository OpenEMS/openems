package io.openems.edge.timedata.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class TimerangesTest {

	@Test
	public void testBuffer() {
		final var timeranges = new Timeranges();
		timeranges.insert(100);
		final var bufferedTimeranges = timeranges.withBuffer(50, 50);
		final var timerangeList = bufferedTimeranges.getTimerangeAscending();

		assertEquals(1, timerangeList.size());

		final var timerange = timerangeList.get(0);
		final var timestamps = timerange.getTimestamps();
		assertEquals(3, timestamps.size());
		assertEquals(50L, timestamps.get(0).longValue());
		assertEquals(100L, timestamps.get(1).longValue());
		assertEquals(150L, timestamps.get(2).longValue());
	}

	@Test
	public void testTimerangesGetTimerangeAscending() {
		final var timeranges = new Timeranges();
		timeranges.insert(0L);
		timeranges.insert(10L);
		timeranges.insert(999999L);
		timeranges.insert(9L);
		timeranges.insert(3L);
		timeranges.insert(11L);

		final var timestamps = timeranges.getTimerangeAscending().stream() //
				.flatMap(t -> t.getTimestamps().stream()) //
				.toList();

		assertEquals(6, timestamps.size());
		assertEquals(0L, timestamps.get(0).longValue());
		assertEquals(3L, timestamps.get(1).longValue());
		assertEquals(9L, timestamps.get(2).longValue());
		assertEquals(10L, timestamps.get(3).longValue());
		assertEquals(11L, timestamps.get(4).longValue());
		assertEquals(999999L, timestamps.get(5).longValue());
	}

	@Test
	public void testTimerangesMaxDataInTime() {
		final var timeranges = new Timeranges();
		timeranges.insert(0L);
		timeranges.insert(100L);
		timeranges.insert(125L);
		timeranges.insert(150L);
		timeranges.insert(200L);
		timeranges.insert(600L);

		final var iter = timeranges.maxDataInTime(50L).iterator();
		assertTrue(iter.hasNext());
		final var firstTimerange = iter.next();
		assertEquals(0L, firstTimerange.getMinTimestamp());
		assertEquals(0L, firstTimerange.getMaxTimestamp());
		assertEquals(1, firstTimerange.getTimestamps().size());
		assertTrue(iter.hasNext());
		final var secondTimerange = iter.next();
		assertEquals(100L, secondTimerange.getMinTimestamp());
		assertEquals(150L, secondTimerange.getMaxTimestamp());
		assertEquals(3, secondTimerange.getTimestamps().size());
		assertTrue(iter.hasNext());
		final var thirdTimerange = iter.next();
		assertEquals(200L, thirdTimerange.getMinTimestamp());
		assertEquals(200L, thirdTimerange.getMaxTimestamp());
		assertEquals(1, thirdTimerange.getTimestamps().size());
		assertTrue(iter.hasNext());
		final var fourthTimerange = iter.next();
		assertEquals(600L, fourthTimerange.getMinTimestamp());
		assertEquals(600L, fourthTimerange.getMaxTimestamp());
		assertEquals(1, fourthTimerange.getTimestamps().size());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testTimerangeGetTimespan() {
		final var timerange = new Timeranges.Timerange(0L);
		timerange.insert(100L);
		assertEquals(100L, timerange.getTimespan());
	}

	@Test
	public void testTimerangeGetMinTimestamp() {
		final var timerange = new Timeranges.Timerange(0L);
		timerange.insert(100L);
		assertEquals(0L, timerange.getMinTimestamp());
	}

	@Test
	public void testTimerangeGetMaxTimestamp() {
		final var timerange = new Timeranges.Timerange(0L);
		timerange.insert(100L);
		assertEquals(100L, timerange.getMaxTimestamp());
	}

	@Test
	public void testTimerangeMaxRange() {
		final var timerange = new Timeranges.Timerange(0L);
		timerange.insert(100L);
		timerange.insert(150L);

		final var iter = timerange.maxRange(50L).iterator();
		assertTrue(iter.hasNext());
		final var firstTimerange = iter.next();
		assertEquals(0L, firstTimerange.getMinTimestamp());
		assertEquals(0L, firstTimerange.getMaxTimestamp());
		assertEquals(1, firstTimerange.getTimestamps().size());
		assertTrue(iter.hasNext());
		final var secondTimerange = iter.next();
		assertEquals(100L, secondTimerange.getMinTimestamp());
		assertEquals(150L, secondTimerange.getMaxTimestamp());
		assertEquals(2, secondTimerange.getTimestamps().size());
		assertFalse(iter.hasNext());
	}

	@Test
	public void testTimerangeCompareTo() {
		final var timerange1 = new Timeranges.Timerange(100L);
		final var timerange2 = new Timeranges.Timerange(100L);
		final var timerange3 = new Timeranges.Timerange(101L + Timeranges.MAX_CONCAT_TIME);

		assertEquals(0, Timeranges.TIMERANGE_COMPARATOR.compare(timerange1, timerange2));
		assertEquals(1, Timeranges.TIMERANGE_COMPARATOR.compare(timerange1, timerange3));
		assertEquals(-1, Timeranges.TIMERANGE_COMPARATOR.compare(timerange3, timerange1));
	}
}
