package io.openems.edge.energy.api.simulation.periods;

import static io.openems.common.utils.DoubleUtils.getOrNull;
import static io.openems.edge.energy.api.EnergyConstants.SCHEDULE_PERIODS_ON_EMPTY;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import io.openems.common.utils.DoubleUtils;
import io.openems.common.utils.IntUtils;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GocUtils;

public class PeriodsBuilder {

	private final Environment environment;
	private final int emptyPeriodsLimit;
	private final Function<ZonedDateTime, Integer> quarterToHourSwitchIndexCalculator;

	private final List<RawPeriod> rawPeriods = new ArrayList<>();

	public PeriodsBuilder(Environment environment) {
		this(environment, SCHEDULE_PERIODS_ON_EMPTY, GocUtils::calculatePeriodDurationHourFromIndex);
	}

	@VisibleForTesting
	PeriodsBuilder(//
			Environment environment, //
			int emptyPeriodsLimit, //
			Function<ZonedDateTime, Integer> quarterToHourSwitchIndexCalculator) {
		this.environment = environment;
		this.emptyPeriodsLimit = emptyPeriodsLimit;
		this.quarterToHourSwitchIndexCalculator = quarterToHourSwitchIndexCalculator;
	}

	/**
	 * Tries to add the given period to the internal list.
	 *
	 * <p>
	 * The period is only added if it satisfies the current validation rules.
	 *
	 * @param period the period to add
	 * @return {@code true} if the period was added, {@code false} otherwise
	 */
	public boolean tryAddPeriod(RawPeriod period) {
		if (!this.shouldAddPeriod(period)) {
			return false;
		}

		this.addPeriod(period);
		return true;
	}

	/**
	 * Adds a new period if it passes validation.
	 *
	 * @param time             the timestamp of the period
	 * @param gridBuySoftLimit the optional grid buy limit
	 * @param production       the production value
	 * @param consumption      the consumption value
	 * @param gridBuyPrice     the grid buy price
	 * @param gridSellPrice    the grid sell price
	 * @return this builder instance for chaining
	 */
	@VisibleForTesting
	public PeriodsBuilder addPeriodIfValid(ZonedDateTime time, Integer gridBuySoftLimit, //
			int production, Integer consumption, Double gridBuyPrice, Double gridSellPrice) {
		final var rawPeriodData = new RawPeriod.RawPeriodData(production, consumption, gridBuyPrice, gridSellPrice);
		final var rawPeriod = new RawPeriod(time, gridBuySoftLimit, rawPeriodData);
		if (this.shouldAddPeriod(rawPeriod)) {
			this.addPeriod(rawPeriod);
		}
		return this;
	}

	public Periods build() {
		if (this.rawPeriods.isEmpty()) {
			return Periods.empty();
		}

		final var firstPeriodTime = this.rawPeriods.getFirst().time();
		final var periodLengthHourFromIndex = this.quarterToHourSwitchIndexCalculator.apply(firstPeriodTime);

		final var gridBuyPriceFactory = this.createPriceFactory(p -> p.data().gridBuyPrice());
		final var gridSellPriceFactory = this.createPriceFactory(p -> p.data().gridSellPrice());

		final var periods = ImmutableList.<GlobalOptimizationContext.Period>builder();
		int i = 0;
		while (i < this.rawPeriods.size()) {
			if (i < periodLengthHourFromIndex) {
				// Add QUARTER
				final var rawPeriod = this.rawPeriods.get(i);
				final var periodData = this.toPeriodData(rawPeriod.data(), gridBuyPriceFactory, gridSellPriceFactory);
				periods.add(new GlobalOptimizationContext.Period.Quarter(i, rawPeriod.time(),
						rawPeriod.gridBuySoftLimit(), periodData));
				i += 1;
			} else {
				// Add HOUR
				Integer gridBuySoftLimitSum = null;
				final var quarterPeriodsBuilder = ImmutableList.<GlobalOptimizationContext.Period.Quarter>builderWithExpectedSize(
						4);
				for (var j = 0; j < 4; j++) {
					int index = i + j;
					if (index < this.rawPeriods.size()) {
						final var rawPeriod = this.rawPeriods.get(index);
						final var periodData = this.toPeriodData(rawPeriod.data(), gridBuyPriceFactory,
								gridSellPriceFactory);

						if (rawPeriod.gridBuySoftLimit() != null) {
							if (gridBuySoftLimitSum == null) {
								gridBuySoftLimitSum = 0;
							}
							gridBuySoftLimitSum += rawPeriod.gridBuySoftLimit();
						}

						quarterPeriodsBuilder.add(new GlobalOptimizationContext.Period.Quarter(j, rawPeriod.time(),
								rawPeriod.gridBuySoftLimit(), periodData));
					}
				}
				final var quarterPeriods = quarterPeriodsBuilder.build();

				final var time = quarterPeriods.getFirst().time();
				var data = this.calculatePeriodDataForHour(quarterPeriods, gridBuyPriceFactory, gridSellPriceFactory);

				final var index = periodLengthHourFromIndex + (i - periodLengthHourFromIndex) / 4;
				periods.add(new GlobalOptimizationContext.Period.Hour(index, time, gridBuySoftLimitSum, data,
						quarterPeriods));
				i += 4;
			}
		}

		return Periods.of(periods.build());
	}

	private void addPeriod(RawPeriod period) {
		if (this.alreadyExists(period)) {
			throw new IllegalArgumentException(String.format(//
					"Duplicated period for time [%s] with %s", period.time(), period));
		}
		this.rawPeriods.add(period);
	}

	private boolean shouldAddPeriod(RawPeriod next) {
		if (this.rawPeriods.isEmpty()) {
			// Always add first period
			return true;
		}

		final var firstPeriodData = this.rawPeriods.getFirst().data();
		if (firstPeriodData.hasNoOptionalValues()) {
			// If first period was empty, add until we have reached the limit
			return this.rawPeriods.size() < this.emptyPeriodsLimit;
		}

		// Check if any value that was at first present is now missing
		return next.data().isCompatibleWith(firstPeriodData);
	}

	private boolean alreadyExists(RawPeriod period) {
		return this.rawPeriods.stream().anyMatch(p -> p.time().isEqual(period.time()));
	}

	private Function<Double, PeriodData.Price> createPriceFactory(Function<RawPeriod, Double> priceGetter) {
		Double min = null;
		Double max = null;

		for (var period : this.rawPeriods) {
			var price = priceGetter.apply(period);
			if (price == null) {
				continue;
			}

			if (min == null || price < min) {
				min = price;
			}
			if (max == null || price > max) {
				max = price;
			}
		}

		return createPriceFactory(min, max);
	}

	private static Function<Double, PeriodData.Price> createPriceFactory(Double min, Double max) {
		if (min == null || max == null) {
			return price -> null;
		}

		final double positiveShift = calculatePositiveShift(min, max);

		return price -> {
			if (price == null) {
				return null;
			}

			final double normalized = DoubleUtils.normalize(price, min, max, 0, 1, false);
			final double positiveShifted = price + positiveShift;

			return new PeriodData.Price(price, normalized, positiveShifted);
		};
	}

	@VisibleForTesting
	static double calculatePositiveShift(double min, double max) {
		if (min >= 0) {
			return 0.0;
		}

		final double range = max - min;
		// The offset values (range / 2.0 and 1e-6) are chosen arbitrarily to ensure
		// the minimum price has sufficient distance to zero. They have no
		// domain-specific reasoning and are only intended to provide a clear separation
		// from zero, ensuring that "not buying" (cost 0) remains preferable to any
		// buying action.
		return -min + (range > 0 ? range / 2.0 : 1e-6);
	}

	private PeriodData toPeriodData(//
			RawPeriod.RawPeriodData rawPeriodData, //
			Function<Double, PeriodData.Price> gridBuyPriceFactory, //
			Function<Double, PeriodData.Price> gridSellPriceFactory) {
		final var gridBuyPrice = gridBuyPriceFactory.apply(rawPeriodData.gridBuyPrice());
		final var gridSellPrice = gridSellPriceFactory.apply(rawPeriodData.gridSellPrice());
		final var consumption = this.toConsumptionPrediction(rawPeriodData.consumption(), gridBuyPrice);

		final var periodDataBuilder = PeriodData.builder()//
				.withProduction(rawPeriodData.production());

		final var firstRawPeriodData = this.rawPeriods.getFirst().data();
		if (firstRawPeriodData.consumption() != null) {
			periodDataBuilder.withConsumption(consumption);
		}
		if (firstRawPeriodData.gridBuyPrice() != null) {
			periodDataBuilder.withGridBuyPrice(gridBuyPrice);
		}
		if (firstRawPeriodData.gridSellPrice() != null) {
			periodDataBuilder.withGridSellPrice(gridSellPrice);
		}

		return periodDataBuilder.build();
	}

	private PeriodData.Prediction toConsumptionPrediction(Integer consumption, PeriodData.Price gridBuyPrice) {
		if (consumption == null) {
			return null;
		}

		final double factor = gridBuyPrice == null //
				? 1.0 //
				: this.environment.consumptionFunction.apply(gridBuyPrice.normalized());

		return new PeriodData.Prediction(consumption, (int) Math.round(consumption * factor));
	}

	private PeriodData calculatePeriodDataForHour(//
			ImmutableList<GlobalOptimizationContext.Period.Quarter> quarters, //
			Function<Double, PeriodData.Price> gridBuyPriceFactory, //
			Function<Double, PeriodData.Price> gridSellPriceFactory) {
		final var productionSum = quarters.stream()//
				.mapToInt(q -> q.data().production())//
				.sum();
		final var consumptionSum = IntUtils.getOrNull(quarters.stream()//
				.map(q -> q.data().consumption())//
				.flatMap(Optional::stream)//
				.mapToInt(PeriodData.Prediction::actual)//
				.reduce(Integer::sum));
		final var gridBuyPriceAvg = getOrNull(quarters.stream()//
				.map(q -> q.data().gridBuyPrice())//
				.flatMap(Optional::stream)//
				.mapToDouble(PeriodData.Price::actual)//
				.average());
		final var gridSellPriceAvg = getOrNull(quarters.stream()//
				.map(q -> q.data().gridSellPrice())//
				.flatMap(Optional::stream)//
				.mapToDouble(PeriodData.Price::actual)//
				.average());

		final var rawPeriodData = new RawPeriod.RawPeriodData(productionSum, consumptionSum, gridBuyPriceAvg,
				gridSellPriceAvg);
		return this.toPeriodData(rawPeriodData, gridBuyPriceFactory, gridSellPriceFactory);
	}
}
