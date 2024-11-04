package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.sortByScheduler;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;

import org.junit.Test;

import io.openems.common.test.TimeLeapClock;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.test.DummyEnergySchedulable;
import io.openems.edge.scheduler.api.test.DummyScheduler;

public class UtilsTest {

	@Test
	public void testCalculateExecutionLimitSeconds() {
		final var clock = new TimeLeapClock(Instant.parse("2022-01-01T00:00:00.00Z"), ZoneOffset.UTC);
		assertEquals(Duration.ofMinutes(14).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(11, ChronoUnit.MINUTES);
		assertEquals(Duration.ofMinutes(3).plusSeconds(30).toSeconds(), calculateExecutionLimitSeconds(clock));

		clock.leap(150, ChronoUnit.SECONDS);
		assertEquals(60, calculateExecutionLimitSeconds(clock));

		clock.leap(1, ChronoUnit.SECONDS);
		assertEquals(Duration.ofMinutes(15).plusSeconds(59).toSeconds(), calculateExecutionLimitSeconds(clock));
	}

	@Test
	public void testSortByScheduler() {
		final var scheduler = new DummyScheduler("scheduler0") //
				.setControllers("d", "f", null, "b");
		final var list = Stream.of("a", "b", "c", "d", "e") //
				.<EnergySchedulable>map(id -> new DummyEnergySchedulable(id, null)) //
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
