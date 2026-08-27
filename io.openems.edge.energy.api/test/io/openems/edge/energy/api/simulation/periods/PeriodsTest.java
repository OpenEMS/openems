package io.openems.edge.energy.api.simulation.periods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;

public class PeriodsTest {

	private static final ZonedDateTime BASE_TIME = ZonedDateTime.parse("2026-01-01T00:00:00Z");

	@Nested
	@DisplayName("of()")
	public class OfTest {

		@Test
		public void shouldCreatePeriods() {
			final var periods = ImmutableList.<Period>of(//
					new Period.Quarter(0, time(0, 0), null, completeData()), //
					new Period.Quarter(1, time(0, 15), null, completeData()), //
					new Period.Hour(2, time(0, 30), null, completeData(), null), //
					new Period.Hour(3, time(1, 30), null, completeData(), null));

			final var result = Periods.of(periods);

			assertEquals(periods.size(), result.size());
			assertEquals(time(0, 0), result.getFirst().time());
		}

		@Test
		public void shouldCreatePeriods_whenSinglePeriod() {
			final var periods = ImmutableList.<Period>of(//
					new Period.Quarter(0, time(0, 0), null, completeData()));

			final var result = Periods.of(periods);

			assertEquals(periods.size(), result.size());
			assertEquals(time(0, 0), result.getFirst().time());
		}

		@Test
		public void shouldThrowException_whenNull() {
			final var exception = assertThrows(NullPointerException.class, () -> {
				Periods.of(null);
			});
			assertTrue(exception.getMessage().contains("Periods must not be null"));
		}

		@Test
		public void shouldCreateEmpty_whenEmpty() {
			final var periods = ImmutableList.<Period>of();

			final var result = Periods.of(periods);

			assertTrue(result.isEmpty());
		}

		@Test
		public void shouldThrowException_whenDuplicatedTimestamps() {
			final var t0 = time(0, 0);
			final var periods = ImmutableList.<Period>of(//
					new Period.Quarter(0, t0, null, completeData()), //
					new Period.Quarter(1, t0, null, completeData()));

			final var exception = assertThrows(Periods.InvalidPeriodsException.class, () -> {
				Periods.of(periods);
			});
			assertTrue(exception.getMessage().contains("Duplicate timestamp"));
		}

		@Test
		public void shouldThrowException_whenGapInTimestamps() {
			final var t0 = time(0, 0);
			final var t1 = time(0, 30); // 15 min gap
			final var periods = ImmutableList.<Period>of(//
					new Period.Quarter(0, t0, null, completeData()), //
					new Period.Quarter(1, t1, null, completeData()));

			final var exception = assertThrows(Periods.InvalidPeriodsException.class, () -> {
				Periods.of(periods);
			});
			assertTrue(exception.getMessage().contains("Invalid timestamp sequence"));
		}

		@Test
		public void shouldThrowException_whenPeriodDataIsInconsistent() {
			final var periods = ImmutableList.<Period>of(//
					new Period.Quarter(0, time(0, 0), null, completeData()), //
					new Period.Quarter(1, time(0, 15), null, incompleteData()));

			final var exception = assertThrows(Periods.InvalidPeriodsException.class, () -> {
				Periods.of(periods);
			});
			assertTrue(exception.getMessage().contains("Inconsistent period data"));
		}
	}

	private static ZonedDateTime time(int hours, int minutes) {
		return BASE_TIME.plusHours(hours).plusMinutes(minutes);
	}

	private static PeriodData completeData() {
		return PeriodData.builder()//
				.withProduction(1000)//
				.withConsumption(new PeriodData.Prediction(500, 500))//
				.withGridBuyPrice(new PeriodData.Price(100, 0, 100))//
				.withGridSellPrice(new PeriodData.Price(25, 0, 25))//
				.build();
	}

	private static PeriodData incompleteData() {
		return PeriodData.builder()//
				.withProduction(1000)//
				.withConsumption(new PeriodData.Prediction(500, 500))//
				.withGridSellPrice(new PeriodData.Price(25, 0, 25))//
				.build();
	}
}
