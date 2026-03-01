package io.openems.edge.energy.optimizer;

import static io.openems.common.test.TestUtils.createDummyClock;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.calculateSleepMillis;
import static io.openems.edge.energy.optimizer.Utils.sortByScheduler;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.common.utils.DateUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.scheduler.api.test.DummyScheduler;

public class UtilsTest {

	@Test
	public void testCalculateSleepMillis() {
		final var clock = createDummyClock();
		assertEquals(Duration.ofMinutes(15).toMillis() + 100, calculateSleepMillis(clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertEquals(Duration.ofMinutes(4).toMillis() + 100, calculateSleepMillis(clock));
	}

	@Test
	public void testCalculateExecutionLimitSeconds() {
		final var clock = createDummyClock();
		assertEquals(Duration.ofMinutes(14).plusSeconds(55).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertEquals(Duration.ofMinutes(3).plusSeconds(55).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(150, ChronoUnit.SECONDS);
		assertEquals(85, calculateExecutionLimitSeconds(clock));

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(84, calculateExecutionLimitSeconds(clock));
	}

	@Test
	public void testCalculateAdjustedDelay_ShouldReturnRequestedDelay_WhenSmallerThanMaxAllowed() {
		var clock = Clock.fixed(Instant.parse("2026-01-13T10:15:30Z"), ZoneId.of("UTC"));
		var requestedDelay = Duration.ofSeconds(5);

		var adjustedDelay = Utils.calculateAdjustedDelay(clock, requestedDelay);

		assertEquals(requestedDelay, adjustedDelay);
	}

	@Test
	public void testCalculateAdjustedDelay_ShouldReturnAdjustedDelay_WhenGreaterThanMaxAllowed() {
		var clock = Clock.fixed(Instant.parse("2026-01-13T10:28:30Z"), ZoneId.of("UTC"));
		var requestedDelay = Duration.ofSeconds(31);

		var adjustedDelay = Utils.calculateAdjustedDelay(clock, requestedDelay);

		var expectedDelay = DateUtils.durationUntilNextQuarter(clock).plus(Optimizer.BUFFER_START);
		assertEquals(expectedDelay, adjustedDelay);
	}

	@Test
	public void testSortByScheduler() {
		final var scheduler = new DummyScheduler("scheduler0") //
				.setControllers("d", "f", null, "b");
		final var list = Stream.of("a", "b", "c", "d", "e") //
				.<EnergySchedulable>map(
						id -> new DummyEnergySchedulable<EnergyScheduleHandler>("Controller.Dummy", id, (cmp) -> null)) //
				.toList();

		var result = sortByScheduler(scheduler, list).stream() //
				.map(EnergySchedulable::id) //
				.toArray();

		assertArrayEquals(//
				new String[] { //
						"d", "b", // by Scheduler
						"a", "c", "e" // remaining alphabetically
				}, result);
	}
}
