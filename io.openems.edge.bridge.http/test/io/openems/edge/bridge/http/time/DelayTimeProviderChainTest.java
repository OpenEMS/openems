package io.openems.edge.bridge.http.time;

import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.fixedAtEveryFull;
import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.fixedDelay;
import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.immediate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.timedata.DurationUnit;

public class DelayTimeProviderChainTest {

	@Test
	public void testImmediate() {
		final var delayProvider = immediate();
		assertEquals(Duration.ofSeconds(0), delayProvider.getDelay());
	}

	@Test
	public void testFixedDelay() {
		final var delay = Duration.ofSeconds(9);
		final var delayProvider = fixedDelay(delay);
		assertEquals(delay, delayProvider.getDelay());
	}

	@Test
	public void testFixedAtEveryFull() {
		final var clock = new TimeLeapClock(LocalDateTime.of(2000, 1, 1, 12, 30, 23).toInstant(ZoneOffset.UTC));
		final var delayProvider = fixedAtEveryFull(clock, DurationUnit.ofMinutes(1));
		assertEquals(Duration.ofSeconds(60 - 23), delayProvider.getDelay());
	}

	@Test
	public void testPlusFixedAmountDelayTimeProviderChainIntTimeUnit() {
		final var delayProvider = fixedDelay(Duration.ofSeconds(5)) //
				.plusFixedAmount(Duration.ofSeconds(3));
		assertEquals(Duration.ofSeconds(8), delayProvider.getDelay());
	}

	@Test
	public void testPlusRandomDelayDelayTimeProviderChainIntTimeUnit() {
		final var delayProvider = fixedDelay(Duration.ofSeconds(5)) //
				.plusRandomDelay(10, ChronoUnit.SECONDS);
		final var createdDelay = delayProvider.getDelay();
		assertTrue(createdDelay.toSeconds() >= 5);
		assertTrue(createdDelay.toSeconds() < 15);
	}

}
