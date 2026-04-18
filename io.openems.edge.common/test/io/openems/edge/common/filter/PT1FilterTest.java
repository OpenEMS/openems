package io.openems.edge.common.filter;

import static org.junit.Assert.assertEquals;

import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

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
		assertEquals(1555, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1703, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1802, sut.applyPT1Filter(2000));

		// Very short: input = output
		clock.leap(50, ChronoUnit.MILLIS);
		assertEquals(1802, sut.applyPT1Filter(2000));

		clock.leap(500, ChronoUnit.MILLIS);
		assertEquals(1872, sut.applyPT1Filter(2000));

		// Reset + first run after reset: input = output
		sut.reset();
		assertEquals(2000, sut.applyPT1Filter(2000));
	}

	@Test
	public void testAntiWindup() {
		// timeConstantMs=1000ms, cycleTimeMs=500ms → factor=2.0
		// formula: output = (value + 2 * lastOutput) / 3
		var cycleTimeMs = 500;
		var timeConstantMs = 1000;
		final var clock = TestUtils.createDummyClock();
		final var sut = new PT1Filter(clock, timeConstantMs);

		final var maxPower = 5000;
		final var lowLimit = 0;
		final var powerAboveLimit = 10_000;
		final var powerBelowLimit = 3_000;
		sut.setLimits(lowLimit, maxPower);

		clock.leap(cycleTimeMs, ChronoUnit.MILLIS);
		assertEquals(maxPower, sut.applyPT1Filter(powerAboveLimit));

		clock.leap(cycleTimeMs, ChronoUnit.MILLIS);
		assertEquals(maxPower, sut.applyPT1Filter(powerAboveLimit));

		// (3000 + 2*5000)/3 = 4333 — without anti-windup: (3000 + 2*6667)/3 = 5445 →
		// still clamped to 5000
		clock.leap(cycleTimeMs, ChronoUnit.MILLIS);
		assertEquals(4333, sut.applyPT1Filter(powerBelowLimit));
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
