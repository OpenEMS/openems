package io.openems.edge.controller.ess.timeofusetariff;

import static io.openems.edge.controller.ess.timeofusetariff.EnergyScheduler.MINIMUM_POWER_FACTOR;
import static java.lang.Math.ceil;
import static java.lang.Math.round;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.annotations.VisibleForTesting;

import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.periods.PeriodDuration;

final class GridOptimizedChargeUtils {

	private GridOptimizedChargeUtils() {
	}

	/**
	 * Determines how many subsequent quarter-hours on the same day have a positive
	 * surplus and also calculates the target time per day based on the available
	 * surplus energy.
	 *
	 * @param goc             the {@link GlobalOptimizationContext}
	 * @param targetSocBuffer the target SoC buffer as a fraction between 0.0 and
	 *                        1.0 (e.g. 0.2 for 20%)
	 * @return a {@link PrecomputeLimitChargeResult} containing the number of future
	 *         surplus quarters for each quarter-hour as well as the calculated
	 *         target charging time for each day
	 */
	static PrecomputeLimitChargeResult precomputeLimitChargeWithSocBuffer(GlobalOptimizationContext goc,
			double targetSocBuffer) {
		if (targetSocBuffer < 0.0 || targetSocBuffer > 1.0) {
			throw new IllegalArgumentException(
					"targetSocBuffer must be between 0.0 and 1.0, but was: " + targetSocBuffer);
		}

		final var allQuartersByDay = getAllQuartersByDay(goc);
		final var surplusQuartersByDay = getSurplusQuartersByDay(allQuartersByDay);

		final int targetEnergyBuffer = (int) round(goc.ess().totalEnergy() * targetSocBuffer);
		final var targetTimeByDay = calculateTargetTimeByDay(surplusQuartersByDay, targetEnergyBuffer);

		final var futureSurplusCounts = computeFutureSurplusCounts(allQuartersByDay, surplusQuartersByDay,
				targetTimeByDay);

		return new PrecomputeLimitChargeResult(futureSurplusCounts, targetTimeByDay);
	}

	/**
	 * Determines how many subsequent quarter-hours on the same day have a positive
	 * surplus using a fixed target time for all days.
	 *
	 * @param goc        the {@link GlobalOptimizationContext}
	 * @param targetTime the fixed target time
	 * @return a {@link PrecomputeLimitChargeResult} containing the number of future
	 *         surplus quarters for each quarter-hour as well as the calculated
	 *         target charging time for each day
	 */
	static PrecomputeLimitChargeResult precomputeLimitChargeWithFixedTargetTime(GlobalOptimizationContext goc,
			LocalTime targetTime) {
		final var allQuartersByDay = getAllQuartersByDay(goc);
		final var surplusQuartersByDay = getSurplusQuartersByDay(allQuartersByDay);

		final var targetTimeByDay = allQuartersByDay.keySet().stream()//
				.collect(Collectors.toMap(day -> day, day -> targetTime));

		final var futureSurplusCounts = computeFutureSurplusCounts(allQuartersByDay, surplusQuartersByDay,
				targetTimeByDay);

		return new PrecomputeLimitChargeResult(futureSurplusCounts, targetTimeByDay);
	}

	record PrecomputeLimitChargeResult(//
			Map<LocalDateTime, Integer> futureSurplusCounts, //
			Map<LocalDate, LocalTime> targetTimeByDay) {
	}

	@VisibleForTesting
	static Map<LocalDateTime, Integer> computeFutureSurplusCounts(//
			Map<LocalDate, List<QuarterlySurplus>> allQuartersByDay, //
			Map<LocalDate, List<QuarterlySurplus>> surplusQuartersByDay, //
			Map<LocalDate, LocalTime> targetTimeByDay) {
		return allQuartersByDay.entrySet().stream()//
				.flatMap(entry -> {
					final var day = entry.getKey();
					final var allQuarters = entry.getValue();
					final var surplusQuarters = surplusQuartersByDay.get(day);
					final var targetTime = targetTimeByDay.get(day);

					if (surplusQuarters == null || surplusQuarters.isEmpty() || targetTime == null) {
						return allQuarters.stream()//
								.map(q -> Map.entry(LocalDateTime.of(day, q.time()), 0));
					}

					return allQuarters.stream()//
							.map(q -> Map.entry(//
									LocalDateTime.of(day, q.time()), //
									(int) surplusQuarters.stream()//
											.filter(sq -> sq.time().isAfter(q.time()) //
													&& !sq.time().isAfter(targetTime))//
											.count()));
				})//
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	@VisibleForTesting
	static Stream<QuarterlySurplus> toQuarterlySurplus(GlobalOptimizationContext.Period period) {
		final var baseTime = period.time().toLocalTime();
		final int surplusEnergy = period.data().production() - period.data().consumption().orElseThrow().actual();

		return switch (period.duration()) {
		case QUARTER -> Stream.of(new QuarterlySurplus(baseTime, surplusEnergy));
		case HOUR -> IntStream.range(0, 4)//
				.mapToObj(i -> new QuarterlySurplus(//
						baseTime.plusMinutes(15L * i), //
						(int) round(surplusEnergy / 4.0)));
		};
	}

	@VisibleForTesting
	static Map<LocalDate, LocalTime> calculateTargetTimeByDay(
			Map<LocalDate, List<QuarterlySurplus>> surplusQuartersByDay, int targetEnergyBuffer) {
		return surplusQuartersByDay.entrySet().stream()//
				.flatMap(entry -> {
					final var quarters = entry.getValue();
					int accEnergy = 0;
					for (int i = quarters.size() - 1; i >= 0; i--) {
						accEnergy += quarters.get(i).surplusEnergy();
						if (accEnergy >= targetEnergyBuffer) {
							return Stream.of(Map.entry(entry.getKey(), quarters.get(i).time()));
						}
					}
					return Stream.empty();
				})//
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	private static Map<LocalDate, List<QuarterlySurplus>> getAllQuartersByDay(GlobalOptimizationContext goc) {
		return goc.periods().stream()//
				.filter(p -> p.data().consumption().isPresent())//
				.collect(Collectors.groupingBy(p -> p.time().toLocalDate()))//
				.entrySet().stream()//
				.collect(Collectors.toMap(//
						Map.Entry::getKey, //
						entry -> entry.getValue().stream()//
								.flatMap(GridOptimizedChargeUtils::toQuarterlySurplus)//
								.sorted(Comparator.comparing(QuarterlySurplus::time))//
								.toList()));
	}

	private static Map<LocalDate, List<QuarterlySurplus>> getSurplusQuartersByDay(
			Map<LocalDate, List<QuarterlySurplus>> allQuartersByDay) {
		return allQuartersByDay.entrySet().stream()//
				.collect(Collectors.toMap(//
						Map.Entry::getKey, //
						e -> e.getValue().stream()//
								.filter(qs -> qs.surplusEnergy() > 0)//
								.toList()));
	}

	@VisibleForTesting
	record QuarterlySurplus(LocalTime time, int surplusEnergy) {
	}

	/**
	 * Calculates the limit charge power required to charge the remaining battery
	 * capacity within the available time window.
	 *
	 * @param numFutureQuartersWithSurplus number of upcoming 15-minute intervals
	 *                                     with surplus energy
	 * @param soc                          current state of charge (0.0 to 1.0)
	 * @param capacity                     battery capacity in Wh
	 * @param durationUntilNextQuarter     time remaining until the next quarter
	 *                                     interval
	 * @return the calculated charge power limit in W, or null if no surplus
	 *         intervals are available
	 */
	static Integer calculateLimitChargePower(//
			int numFutureQuartersWithSurplus, double soc, int capacity, Duration durationUntilNextQuarter) {
		if (numFutureQuartersWithSurplus <= 0) {
			return null;
		}

		// Calculate remaining capacity in Wh
		final double remainingCapacity = (1 - soc) * capacity;
		if (remainingCapacity <= 0) {
			return null;
		}

		// Calculate remaining time in h
		final var remainingTime = durationUntilNextQuarter.toSeconds() / 3600.0 //
				+ numFutureQuartersWithSurplus / 4.0;

		// Calculate target charge power, ensuring minimum power
		final int targetLimitChargePower = (int) round(remainingCapacity / remainingTime);
		final int minChargePower = (int) ceil(capacity * MINIMUM_POWER_FACTOR);
		if (targetLimitChargePower < minChargePower) {
			return 0;
		}

		return targetLimitChargePower;
	}

	/**
	 * Calculates the limit charge energy required to charge the remaining battery
	 * capacity within the available time window.
	 *
	 * @param numFutureQuartersWithSurplus number of upcoming 15-minute intervals
	 *                                     with surplus energy
	 * @param capacity                     battery capacity in Wh
	 * @param initialEnergy                initial energy at current period start in
	 *                                     Wh
	 * @param periodDuration               the {@link PeriodDuration} of the current
	 *                                     period
	 * @return the calculated charge energy limit in Wh, or null if no surplus
	 *         intervals are available
	 */
	static Integer calculateLimitChargeEnergy(//
			int numFutureQuartersWithSurplus, int capacity, int initialEnergy, PeriodDuration periodDuration) {
		if (numFutureQuartersWithSurplus <= 0) {
			return null;
		}

		// Calculate remaining capacity in Wh
		final int remainingCapacity = capacity - initialEnergy;
		if (remainingCapacity <= 0) {
			return null;
		}

		// Calculate required charge energy per quarter (+ 1 for this period)
		final int requiredEnergyPerQuarter = (int) ceil(
				(double) remainingCapacity / (numFutureQuartersWithSurplus + 1));

		// Adjust energy based on period duration
		final int targetLimitChargeEnergy = switch (periodDuration) {
		case QUARTER -> requiredEnergyPerQuarter;
		case HOUR -> requiredEnergyPerQuarter * 4;
		};

		// Ensure minimum charge power is respected
		final int minChargePower = (int) ceil(capacity * MINIMUM_POWER_FACTOR);
		final int minChargeEnergy = periodDuration.convertPowerToEnergy(minChargePower);
		if (targetLimitChargeEnergy < minChargeEnergy) {
			return 0;
		}

		return targetLimitChargeEnergy;
	}
}
