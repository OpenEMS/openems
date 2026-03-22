package io.openems.edge.common.filter;

import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TestUtils;

public class PT1FilterTest {

	@Test
	public void testDisabled() {
		final var clock = TestUtils.createDummyClock();
		final var sut = new PT1Filter(clock, 0);

		// First run: input = output
		assertEquals(1000, sut.applyPT1Filter(1000));

		// Disabled: input = output
		assertEquals(1000, sut.applyPT1Filter(1000));
	}

	@Test
	public void test() {
		final var clock = TestUtils.createDummyClock();
		final var sut = new PT1Filter(clock, 1000);

		// First run: input = output
		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1000, sut.applyPT1Filter(1000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1333, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1556, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1704, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1802, sut.applyPT1Filter(2000));

		// Very short: input = output
		clock.leap(50, ChronoUnit.MILLIS);
		assertEquals(1802, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1873, sut.applyPT1Filter(2000));

		// Reset + first run after reset: input = output
		sut.reset();
		assertEquals(2000, sut.applyPT1Filter(2000));
	}

	@Test
	public void testUsesFloatingPointDivisionForFactor() {
		final var clock = TestUtils.createDummyClock();
		final var sut = new PT1Filter(clock, 1000);

		clock.leap(600, ChronoUnit.MILLIS);
		assertEquals(1000, sut.applyPT1Filter(1000));

		clock.leap(600, ChronoUnit.MILLIS);
		assertEquals(1375, sut.applyPT1Filter(2000));
	}

}
