package io.openems.edge.bridge.http.time;

import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.fixedAtEveryFull;
import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.fixedDelay;
import static io.openems.edge.bridge.http.time.DelayTimeProviderChain.immediate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.common.timedata.DurationUnit;

public class DelayTimeProviderChainTest {

	@Test
	public void testImmediate() {
		final var delayProvider = immediate();
		assertEquals(0L, delayProvider.getDelay().amount());
	}

	@Test
	public void testFixedDelay() {
		final var delay = new Delay(9, TimeUnit.SECONDS);
		final var delayProvider = fixedDelay(delay);
		assertEquals(delay, delayProvider.getDelay());
	}

	@Test
	public void testFixedAtEveryFull() {
		final var clock = new TimeLeapClock(LocalDateTime.of(2000, 1, 1, 12, 30, 23).toInstant(ZoneOffset.UTC));
		final var delayProvider = fixedAtEveryFull(clock, DurationUnit.ofMinutes(1));
		assertEquals(new Delay((60 - 23) * 1000, TimeUnit.MILLISECONDS), delayProvider.getDelay());
	}

	@Test
	public void testPlusFixedAmountDelayTimeProviderChainIntTimeUnit() {
		final var delayProvider = fixedDelay(new Delay(5, TimeUnit.SECONDS)) //
				.plusFixedAmount(3, TimeUnit.SECONDS);
		assertEquals(new Delay(8, TimeUnit.SECONDS), delayProvider.getDelay());
	}

	@Test
	public void testPlusRandomDelayDelayTimeProviderChainIntTimeUnit() {
		final var delayProvider = fixedDelay(new Delay(5, TimeUnit.SECONDS)) //
				.plusRandomDelay(10, TimeUnit.SECONDS);
		final var createdDelay = delayProvider.getDelay();
		assertEquals(TimeUnit.SECONDS, createdDelay.unit());
		assertTrue(createdDelay.amount() >= 5);
		assertTrue(createdDelay.amount() < 15);
	}

}
