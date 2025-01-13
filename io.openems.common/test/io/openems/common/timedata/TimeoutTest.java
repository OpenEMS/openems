package io.openems.common.timedata;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;

public class TimeoutTest {

	@Test
	public void test() {
		final var timeout = Timeout.ofSeconds(120);
		final var timeLeap = new TimeLeapClock();
		timeout.start(timeLeap);

		timeLeap.leap(20, ChronoUnit.SECONDS);
		assertFalse(timeout.elapsed(timeLeap));

		timeLeap.leap(121, ChronoUnit.SECONDS);
		assertTrue(timeout.elapsed(timeLeap));
	}

}
