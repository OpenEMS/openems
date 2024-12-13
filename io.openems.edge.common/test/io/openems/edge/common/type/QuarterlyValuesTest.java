package io.openems.edge.common.type;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.ZonedDateTime;

import org.junit.Test;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.common.test.TestUtils;

public class QuarterlyValuesTest {

	private static class MyQuarterlyValues extends QuarterlyValues<Double> {

		protected MyQuarterlyValues(ImmutableSortedMap<ZonedDateTime, Double> valuePerQuarter) {
			super(valuePerQuarter);
		}

		protected MyQuarterlyValues(ZonedDateTime time, Double... values) {
			super(time, values);
		}

		protected Double[] asArray() {
			return super.asArray(Double[]::new);
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testExpectError() {
		var time = ZonedDateTime.now(TestUtils.createDummyClock());
		new MyQuarterlyValues(ImmutableSortedMap.of(//
				time.plusMinutes(1), 0.1));
	}

	@Test
	public void testEmpty() {
		var time = ZonedDateTime.now(TestUtils.createDummyClock());
		var sut = new MyQuarterlyValues(time);
		assertTrue(sut.isEmpty());
		assertNull(sut.getFirst());
		assertNull(sut.getFirstTime());
		assertNull(sut.getLastTime());
		assertNull(sut.getAt(time));
		assertEquals("MyQuarterlyValues{EMPTY}", sut.toString());
		assertTrue(sut.toMapWithAllQuarters().isEmpty());
		assertEquals(0, sut.getBetween(time, time.plusMinutes(50)).toList().size());
	}

	@Test
	public void test() {
		var time = ZonedDateTime.now(TestUtils.createDummyClock());
		var sut = new MyQuarterlyValues(ImmutableSortedMap.of(//
				time, 0.1, //
				time.plusMinutes(15), 0.2, //
				time.plusMinutes(30), 0.3, //
				time.plusMinutes(45), 0.4, //
				time.plusMinutes(60), 0.5));
		assertFalse(sut.isEmpty());
		assertEquals(0.1, sut.getFirst(), 0.001);
		assertEquals(time, sut.getFirstTime());
		assertEquals(time.plusMinutes(60), sut.getLastTime());
		assertEquals(0.2, sut.getAt(time.plusMinutes(15)), 0.001);
		assertEquals("MyQuarterlyValues{start=2020-01-01T00:00Z, values=0.1,0.2,0.3,0.4,0.5}", sut.toString());
		assertEquals(4, sut.getBetween(time, time.plusMinutes(50)).toList().size());
	}

	@Test
	public void test2() {
		var time = ZonedDateTime.now(TestUtils.createDummyClock());
		var sut = new MyQuarterlyValues(time, 0.1, 0.2, null, 0.3);
		assertEquals(3, sut.asArray().length);
		assertEquals(4, sut.toMapWithAllQuarters().size());
	}

}
