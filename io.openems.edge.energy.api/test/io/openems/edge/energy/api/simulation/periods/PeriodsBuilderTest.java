package io.openems.edge.energy.api.simulation.periods;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

public class PeriodsBuilderTest {

	private static final ZonedDateTime BASE_TIME = ZonedDateTime.parse("2026-01-01T00:00:00Z");

	@Nested
	@DisplayName("tryAddPeriod()")
	class TryAddPeriodTest {

		@Test
		void shouldAddPeriod_whenFirstPeriod() {
			final var sut = new PeriodsBuilder(Environment.TEST, 96, (t) -> 24);
			final var p0 = new RawPeriod(time(0, 0), null, dataWithAllOptionalValues());

			final boolean p0Added = sut.tryAddPeriod(p0);

			assertTrue(p0Added);
		}

		@Test
		void shouldAddEmptyPeriodsUntilLimitReached() {
			final var sut = new PeriodsBuilder(Environment.TEST, 2 /* small limit */, (t) -> 24);
			final var p0 = new RawPeriod(time(0, 0), null, dataWithNoOptionalValues());
			final var p1 = new RawPeriod(time(0, 15), null, dataWithNoOptionalValues());
			final var p2 = new RawPeriod(time(0, 30), null, dataWithNoOptionalValues());

			final boolean p0Added = sut.tryAddPeriod(p0);
			final boolean p1Added = sut.tryAddPeriod(p1);
			final boolean p2Added = sut.tryAddPeriod(p2);

			assertTrue(p0Added);
			assertTrue(p1Added);
			assertFalse(p2Added);
		}

		@Test
		void shouldStopAddingPeriods_whenPeriodIsNotCompatibleWithFirstPeriod() {
			final var sut = new PeriodsBuilder(Environment.TEST, 96, (t) -> 24);
			final var p0 = new RawPeriod(time(0, 0), null, dataWithAllOptionalValues());
			final var p1 = new RawPeriod(time(0, 15), null, dataWithAllOptionalValues());
			final var p2 = new RawPeriod(time(0, 30), null, dataWithOneMissingValue());

			final boolean p0Added = sut.tryAddPeriod(p0);
			final boolean p1Added = sut.tryAddPeriod(p1);
			final boolean p2Added = sut.tryAddPeriod(p2);

			assertTrue(p0Added);
			assertTrue(p1Added);
			assertFalse(p2Added);
		}

		@Test
		void shouldThrowException_whenDuplicatedTimestamps() {
			final var sut = new PeriodsBuilder(Environment.TEST, 96, (t) -> 24);
			final var p0 = new RawPeriod(time(0, 0), null, dataWithAllOptionalValues());
			final var p1 = new RawPeriod(time(0, 15), null, dataWithAllOptionalValues());
			final var p2 = new RawPeriod(time(0, 15) /* duplicated */, null, dataWithAllOptionalValues());

			final boolean p0Added = sut.tryAddPeriod(p0);
			final boolean p1Added = sut.tryAddPeriod(p1);

			assertTrue(p0Added);
			assertTrue(p1Added);
			final var exception = assertThrows(IllegalArgumentException.class, () -> {
				sut.tryAddPeriod(p2);
			});
			assertTrue(exception.getMessage().contains("Duplicated period"));
		}
	}

	@Nested
	@DisplayName("build()")
	class BuildTest {

		@Test
		void shouldBuildPeriods() {
			final var sut = new PeriodsBuilder(Environment.TEST, 96, (t) -> 2);
			final var periods = List.of(//
					new RawPeriod(time(0, 0), null, dataWithOneMissingValue()), //
					new RawPeriod(time(0, 15), null, dataWithOneMissingValue()), //
					new RawPeriod(time(0, 30), null, dataWithOneMissingValue()), //
					new RawPeriod(time(0, 45), null, dataWithOneMissingValue()), //
					new RawPeriod(time(1, 0), null, dataWithAllOptionalValues()), //
					new RawPeriod(time(1, 15), null, dataWithAllOptionalValues()), //
					new RawPeriod(time(1, 30), null, dataWithAllOptionalValues()), //
					new RawPeriod(time(1, 45), null, dataWithTwoMissingValues()));

			periods.forEach(sut::tryAddPeriod);
			final var result = sut.build();

			assertEquals(4, result.size());

			final var res0 = result.get(0);
			assertInstanceOf(GlobalOptimizationContext.Period.Quarter.class, res0);
			assertEquals(0, res0.index());

			final var res1 = result.get(1);
			assertInstanceOf(GlobalOptimizationContext.Period.Quarter.class, res1);
			assertEquals(1, res1.index());

			final var res2 = result.get(2);
			assertInstanceOf(GlobalOptimizationContext.Period.Hour.class, res2);
			assertEquals(2, res2.index());
			assertEquals(4000, res2.data().production());
			final var consumption2 = res2.data().consumption();
			assertTrue(consumption2.isPresent());
			assertEquals(2000, consumption2.get().actual());
			assertFalse(res2.data().gridBuyPrice().isPresent());
			final var gridSellPrice2 = res2.data().gridSellPrice();
			assertTrue(gridSellPrice2.isPresent());
			assertEquals(25.0, gridSellPrice2.get().actual());

			final var res3 = result.get(3);
			assertInstanceOf(GlobalOptimizationContext.Period.Hour.class, res3);
			assertEquals(3, res3.index());
		}

		@Test
		void shouldReturnEmptyPeriods_whenNoPeriodsAdded() {
			final var sut = new PeriodsBuilder(Environment.TEST, 96, (t) -> 24);

			final var result = sut.build();

			assertTrue(result.isEmpty());
		}
	}

	@Nested
	@DisplayName("calculatePositiveShift()")
	class CalculatePositiveShiftTest {

		@Test
		void shouldReturnZero_whenMinIsNonNegative() {
			final double shift = PeriodsBuilder.calculatePositiveShift(0.0, 10.0);

			assertEquals(0.0, shift, 0.0);
		}

		@Test
		void shouldUseRangeHalf_whenMinIsNegativeAndRangeIsPositive() {
			final double shift = PeriodsBuilder.calculatePositiveShift(-10.0, 20.0);

			assertEquals(25.0, shift, 1e-12);
		}

		@Test
		void shouldUseEpsilon_whenMinIsNegativeAndRangeIsZero() {
			final double shift = PeriodsBuilder.calculatePositiveShift(-10.0, -10.0);

			assertEquals(10.000001, shift, 1e-12);
		}

		@Test
		void shouldMakeMinimumStrictlyPositive_whenMinIsNegativeAndRangeIsZero() {
			final double min = -10.0;
			final double shift = PeriodsBuilder.calculatePositiveShift(min, min);
			final double shiftedMin = min + shift;

			assertEquals(1e-6, shiftedMin, 1e-12);
		}
	}

	private static ZonedDateTime time(int hours, int minutes) {
		return BASE_TIME.plusHours(hours).plusMinutes(minutes);
	}

	private static RawPeriod.RawPeriodData dataWithAllOptionalValues() {
		return new RawPeriod.RawPeriodData(1000, 500, 100.0, 25.0);
	}

	private static RawPeriod.RawPeriodData dataWithNoOptionalValues() {
		return new RawPeriod.RawPeriodData(0, null, null, null);
	}

	private static RawPeriod.RawPeriodData dataWithOneMissingValue() {
		return new RawPeriod.RawPeriodData(1000, 500, null, 25.0);
	}

	private static RawPeriod.RawPeriodData dataWithTwoMissingValues() {
		return new RawPeriod.RawPeriodData(1000, null, null, 25.0);
	}
}
