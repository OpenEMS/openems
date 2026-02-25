package io.openems.edge.common.type;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.common.type.QuarterlyValues.streamQuartersExclusive;
import static io.openems.edge.common.type.QuarterlyValues.streamQuartersInclusive;
import static java.time.temporal.ChronoUnit.MINUTES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Instant;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

public class QuarterlyValuesTest {

	private static class MyQuarterlyValues extends QuarterlyValues<Double> {

		protected MyQuarterlyValues(ImmutableSortedMap<Instant, Double> valuePerQuarter) {
			super(valuePerQuarter);
		}

		protected MyQuarterlyValues(Instant time, Double... values) {
			super(time, values);
		}

		protected Double[] asArray() {
			return super.asArray(Double[]::new);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectError() {
		var time = Instant.now(createDummyClock());
		new MyQuarterlyValues(ImmutableSortedMap.of(//
				time.plus(1, MINUTES), 0.1));
	}

	@Test
	public void testEmpty() {
		var time = Instant.now(createDummyClock());
		var sut = new MyQuarterlyValues(time);
		assertTrue(sut.isEmpty());
		assertNull(sut.getFirst());
		assertNull(sut.getFirstTime());
		assertNull(sut.getLastTime());
		assertNull(sut.getAt(time));
		assertEquals("MyQuarterlyValues{EMPTY}", sut.toString());
		assertTrue(sut.toMapWithAllQuarters().isEmpty());
		assertEquals(0, sut.getBetweenInclusive(time, time.plus(50, MINUTES)).toList().size());
	}

	@Test
	public void test() {
		final var time0 = Instant.now(createDummyClock());
		final var time15 = time0.plus(15, MINUTES);
		final var time23 = time0.plus(23, MINUTES);
		final var time30 = time0.plus(30, MINUTES);
		final var time45 = time0.plus(45, MINUTES);
		final var time60 = time0.plus(60, MINUTES);

		var sut = new MyQuarterlyValues(ImmutableSortedMap.of(//
				time0, 0.1, //
				time15, 0.2, //
				time30, 0.3, //
				time45, 0.4, //
				time60, 0.5));
		assertFalse(sut.isEmpty());
		assertEquals(0.1, sut.getFirst(), 0.001);
		assertEquals(time0, sut.getFirstTime());
		assertEquals(time60, sut.getLastTime());
		assertEquals(0.2, sut.getAt(time15), 0.001);
		assertEquals(0.2, sut.getAt(time23), 0.001); // rounded down
		assertEquals("MyQuarterlyValues{start=2020-01-01T00:00:00Z, values=0.1,0.2,0.3,0.4,0.5}", sut.toString());
		assertEquals(4, sut.getBetweenExclusive(time0, time60).toList().size());

		assertArrayEquals(new Instant[] { time15, time30 }, streamQuartersExclusive(time15, time45).toArray());
		assertArrayEquals(new Instant[] { time15, time30, time45 }, streamQuartersInclusive(time15, time45).toArray());
	}

	@Test
	public void test2() {
		var time = Instant.now(createDummyClock());
		var sut = new MyQuarterlyValues(time, 0.1, 0.2, null, 0.3);
		assertEquals(3, sut.asArray().length);
		assertEquals(4, sut.toMapWithAllQuarters().size());
	}

}
