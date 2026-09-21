package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.calculateLimitChargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.calculateLimitChargePower;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.calculateTargetTimeByDay;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.computeFutureSurplusCounts;
import static io.openems.edge.controller.ess.timeofusetariff.GridOptimizedChargeUtils.toQuarterlySurplus;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.periods.PeriodData;
import io.openems.edge.energy.api.simulation.periods.PeriodDuration;

class GridOptimizedChargeUtilsTest {

	@Nested
	@DisplayName("computeFutureSurplusCounts()")
	class ComputeFutureSurplusCountsTest {

		@Test
		void shouldRespectTargetTime() {
			final var day = LocalDate.of(2026, 3, 23);
			final var quarters = List.of(//
					new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 0), 1), //
					new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 15), 1), //
					new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 30), 0), //
					new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 45), 1), //
					new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(11, 0), 1));

			final var allQuartersByDay = Map.of(day, quarters);
			final var surplusQuartersByDay = Map.of(//
					day, List.of(//
							quarters.get(0), // 10:00
							quarters.get(1), // 10:15
							quarters.get(3), // 10:45
							quarters.get(4))); // 11:00
			final var targetTimeByDay = Map.of(day, LocalTime.of(10, 45));

			final var result = computeFutureSurplusCounts(allQuartersByDay, surplusQuartersByDay, targetTimeByDay);

			assertEquals(5, result.size());
			assertEquals((Integer) 2, result.get(LocalDateTime.of(day, LocalTime.of(10, 0))));
			assertEquals((Integer) 1, result.get(LocalDateTime.of(day, LocalTime.of(10, 15))));
			assertEquals((Integer) 1, result.get(LocalDateTime.of(day, LocalTime.of(10, 30))));
			assertEquals((Integer) 0, result.get(LocalDateTime.of(day, LocalTime.of(10, 45))));
			assertEquals((Integer) 0, result.get(LocalDateTime.of(day, LocalTime.of(11, 0))));
		}

		@Test
		void shouldHandleMultipleDays() {
			final var day1 = LocalDate.of(2026, 3, 23);
			final var day2 = LocalDate.of(2026, 3, 24);

			final var quarterlySurplus1 = new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 0), 1);
			final var quarterlySurplus2 = new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(11, 0), 1);

			final var allQuartersByDay = Map.of(//
					day1, List.of(quarterlySurplus1), //
					day2, List.of(quarterlySurplus2));
			final var surplusQuartersByDay = Map.of(//
					day1, List.of(quarterlySurplus1), //
					day2, List.<GridOptimizedChargeUtils.QuarterlySurplus>of());
			final var targetTimeByDay = Map.of(//
					day1, LocalTime.MAX, //
					day2, LocalTime.MAX);

			final var result = computeFutureSurplusCounts(allQuartersByDay, surplusQuartersByDay, targetTimeByDay);

			assertEquals(2, result.size());
			assertEquals((Integer) 0, result.get(LocalDateTime.of(day1, LocalTime.of(10, 0))));
			assertEquals((Integer) 0, result.get(LocalDateTime.of(day2, LocalTime.of(11, 0))));
		}
	}

	@Nested
	@DisplayName("toQuarterlySurplus()")
	class ToQuarterlySurplusTest {

		@Test
		void shouldReturnSingleQuarter_whenQuarterlyPeriod() {
			final var time = LocalDateTime.of(//
					LocalDate.of(2026, 3, 23), //
					LocalTime.of(10, 0));
			final var periodData = PeriodData.builder()//
					.withProduction(1000)//
					.withConsumption(new PeriodData.Prediction(0, 0))//
					.build();
			final var period = new GlobalOptimizationContext.Period.Quarter(//
					0, time.atZone(ZoneId.of("UTC")), null, periodData);

			final var result = toQuarterlySurplus(period).toList();

			assertEquals(1, result.size());
			assertEquals(new GridOptimizedChargeUtils.QuarterlySurplus(time.toLocalTime(), 1000), result.get(0));
		}

		@Test
		void shouldReturnFourQuarters_whenHourlyPeriod() {
			final var time = LocalDateTime.of(//
					LocalDate.of(2026, 3, 23), //
					LocalTime.of(10, 0));
			final var periodData = PeriodData.builder()//
					.withProduction(1000)//
					.withConsumption(new PeriodData.Prediction(0, 0))//
					.build();
			final var period = new GlobalOptimizationContext.Period.Hour(//
					0, time.atZone(ZoneId.of("UTC")), null, periodData, null);

			final var result = toQuarterlySurplus(period).toList();

			assertEquals(4, result.size());
			assertEquals(new GridOptimizedChargeUtils.QuarterlySurplus(time.toLocalTime(), 250), result.get(0));
			assertEquals(new GridOptimizedChargeUtils.QuarterlySurplus(time.toLocalTime().plusMinutes(15), 250),
					result.get(1));
			assertEquals(new GridOptimizedChargeUtils.QuarterlySurplus(time.toLocalTime().plusMinutes(30), 250),
					result.get(2));
			assertEquals(new GridOptimizedChargeUtils.QuarterlySurplus(time.toLocalTime().plusMinutes(45), 250),
					result.get(3));
		}
	}

	@Nested
	@DisplayName("calculateTargetTimeByDay()")
	class CalculateTargetTimeByDayTest {

		@Test
		void shouldReturnCorrectQuarter_whenBufferReached() {
			final var day = LocalDate.of(2026, 3, 25);
			final var surplusQuartersByDay = Map.of(//
					day, List.of(//
							new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(9, 0), 10), //
							new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(9, 15), 20), //
							new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(9, 30), 5)));
			final int targetEnergyBuffer = 25;

			final var result = calculateTargetTimeByDay(surplusQuartersByDay, targetEnergyBuffer);

			assertEquals(LocalTime.of(9, 15), result.get(day));
		}

		@Test
		void shouldReturnNull_whenBufferNotReached() {
			final var day = LocalDate.of(2026, 3, 25);
			final var surplusQuartersByDay = Map.of(//
					day, List.of(//
							new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 0), 5), //
							new GridOptimizedChargeUtils.QuarterlySurplus(LocalTime.of(10, 15), 5)));
			final int targetEnergyBuffer = 20; // Larger than total surplus

			final var result = calculateTargetTimeByDay(surplusQuartersByDay, targetEnergyBuffer);

			assertNull(result.get(LocalDate.of(2026, 3, 25)));
		}
	}

	@Nested
	@DisplayName("calculateLimitChargePower()")
	class CalculateLimitChargePowerTest {

		@Test
		void shouldReturnNull_whenNoSurplusIntervals() {
			final var result = calculateLimitChargePower(//
					0, 0.5, 10000, Duration.ofMinutes(10));
			assertNull(result);
		}

		@Test
		void shouldReturnNull_whenNoRemainingCapacity() {
			final var result = calculateLimitChargePower(//
					4, 1.0, 10000, Duration.ofMinutes(10));
			assertNull(result);
		}

		@Test
		void shouldReturnCalculatedPower_whenNormalCase() {
			final var result = calculateLimitChargePower(//
					4, 0.5, 10000, Duration.ZERO);
			assertEquals((Integer) 5000, result);
		}

		@Test
		void shouldConsiderInitialDuration_whenDurationUntilNextQuarterExists() {
			final var result = calculateLimitChargePower(//
					4, 0.5, 10000, Duration.ofMinutes(15));
			assertEquals((Integer) 4000, result);
		}

		@Test
		void shouldReturnZero_whenCalculatedPowerIsTooLow() {
			final var result = calculateLimitChargePower(//
					4, 0.99, 10000, Duration.ZERO);
			assertEquals(0, result);
		}
	}

	@Nested
	@DisplayName("calculateLimitChargeEnergy()")
	class CalculateLimitChargeEnergyTest {

		@Test
		void shouldReturnNull_whenNoSurplusIntervals() {
			final Integer result = calculateLimitChargeEnergy(//
					0, 10000, 0, PeriodDuration.QUARTER);
			assertNull(result);
		}

		@Test
		void shouldReturnNull_whenNoRemainingCapacity() {
			final Integer result = calculateLimitChargeEnergy(//
					4, 10000, 10000, PeriodDuration.QUARTER);
			assertNull(result);
		}

		@Test
		void shouldReturnCalculatedEnergy_whenNormalCase() {
			final Integer result = calculateLimitChargeEnergy(//
					4, 10000, 5000, PeriodDuration.QUARTER);
			assertEquals((Integer) 1000, result);
		}

		@Test
		void shouldConsiderHourPeriod() {
			final Integer result = calculateLimitChargeEnergy(//
					4, 10000, 5000, PeriodDuration.HOUR);
			assertEquals((Integer) 4000, result);
		}

		@Test
		void shouldReturnZero_whenCalculatedEnergyIsTooLow() {
			final Integer result = calculateLimitChargeEnergy(//
					4, 10000, 9900, PeriodDuration.QUARTER);
			assertEquals(0, result);
		}
	}
}
