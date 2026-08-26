package io.openems.edge.timeofusetariff.api.utils;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.test.TimeLeapClock;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;

class TimeOfUseDelayTimeProviderTest {

	@Test
	void testDelayTimeProviderBefore16_00() {
		final var clock = new TimeLeapClock(Instant.parse("2000-01-01T10:00:00.00Z"));
		final var delayTimeProvider = new TimeOfUseDelayTimeProvider(clock);

		final var prices = TimeOfUsePrices.from(clock.instant(), 0.0);

		final var delay = assertInstanceOf(DelayTimeProvider.Delay.DurationDelay.class,
				delayTimeProvider.onSuccessRunDelay(prices));

		assertGreaterThan(Duration.ofHours(6), delay.getDuration());
		assertLessThan(Duration.ofHours(6).plusMinutes(1), delay.getDuration());
	}

	@Test
	void testDelayTimeProviderAfter16_00() {
		final var clock = new TimeLeapClock(Instant.parse("2000-01-01T16:10:00.00Z"));
		final var delayTimeProvider = new TimeOfUseDelayTimeProvider(clock);

		final var prices = TimeOfUsePrices.from(clock.instant(), 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
				0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0);

		final var delay = assertInstanceOf(DelayTimeProvider.Delay.DurationDelay.class,
				delayTimeProvider.onSuccessRunDelay(prices));

		assertGreaterThan(Duration.ofHours(23).plusMinutes(50), delay.getDuration());
		assertLessThan(Duration.ofHours(23).plusMinutes(50).plusMinutes(1), delay.getDuration());

	}

	private static void assertLessThan(Duration expected, Duration actual) {
		if (actual.compareTo(expected) > 0) {
			throw new AssertionError("Expected " + actual + " to be less than " + expected);
		}
	}

	private static void assertGreaterThan(Duration expected, Duration actual) {
		if (actual.compareTo(expected) < 0) {
			throw new AssertionError("Expected " + actual + " to be greater than " + expected);
		}
	}

}