package io.openems.edge.energy.api.simulation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.common.utils.DoubleUtils.getOrNull;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.energy.api.EnergyConstants.SCHEDULE_PERIODS_ON_EMPTY;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration.QUARTER;
import static java.lang.Math.abs;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.util.stream.Collectors.joining;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;

import io.openems.common.utils.DateUtils;
import io.openems.common.utils.DoubleUtils;
import io.openems.common.utils.DoubleUtils.DoubleToDoubleFunction;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Ess;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Grid;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Prediction;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Price;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Periods;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUsePrices;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Utils for {@link GlobalOptimizationContext}.
 */
public class GocUtils {

	/**
	 * Builder for Periods.
	 */
	public static class PeriodsBuilder {
		protected static record TmpPeriod(ZonedDateTime time, Integer gridBuySoftLimit, //
				Integer production, Integer consumption, //
				Double price) {
			protected Period.Quarter toPeriodQuarter(Environment environment, int index,
					DoubleToDoubleFunction normalizePrice) {
				final var price = toPrice(this.price, normalizePrice);
				final var prediction = toPrediction(this.production, this.consumption, environment, price);

				if (prediction != null && price != null) {
					return new Period.Quarter.Complete(//
							index, this.time, this.gridBuySoftLimit, prediction, price);
				} else if (price != null) {
					return new Period.Quarter.WithPrice(//
							index, this.time, this.gridBuySoftLimit, price);
				} else if (prediction != null) {
					return new Period.Quarter.WithPrediction(//
							index, this.time, this.gridBuySoftLimit, prediction);
				} else {
					return new Period.Quarter.Empty(//
							index, this.time, this.gridBuySoftLimit);
				}
			}
		}

		private final Environment environment;
		private final List<TmpPeriod> tmpPeriods = new ArrayList<TmpPeriod>();

		protected PeriodsBuilder(Environment environment) {
			this.environment = environment;
		}

		/**
		 * Adds a (temporary) Period.
		 * 
		 * @param time             the {@link ZonedDateTime}
		 * @param gridBuySoftLimit the {@link GridBuySoftLimit}
		 * @param production       the production prediction
		 * @param consumption      the consumption prediction
		 * @param price            the price
		 * @return whether it was actually added
		 */
		public synchronized boolean addIfValid(ZonedDateTime time, Integer gridBuySoftLimit, //
				Integer production, Integer consumption, Double price) {
			if (this.tmpPeriods.stream().anyMatch(tp -> tp.time.isEqual(time))) {
				throw new IllegalArgumentException("Duplicated time [" + time + "] with [production=" + production
						+ "; consumption=" + consumption + "; price=" + price + "]");
			}

			final boolean doAdd;
			if (this.tmpPeriods.isEmpty()) {
				doAdd = true;
			} else {
				final var first = this.tmpPeriods.getFirst();
				if (first.consumption != null && consumption != null && first.price != null && price != null) {
					doAdd = true; // Complete
				} else if (first.consumption != null && consumption == null) {
					doAdd = false; // Predictions were available
				} else if (first.price != null && price == null) {
					doAdd = false; // Prices were available
				} else {
					doAdd = this.tmpPeriods.size() <= SCHEDULE_PERIODS_ON_EMPTY; // Empty
				}
			}
			if (!doAdd) {
				return doAdd;
			}

			var tp = new TmpPeriod(time, gridBuySoftLimit, production, consumption, price);
			this.tmpPeriods.add(tp);
			return true;
		}

		/**
		 * Adds a (temporary) Period.
		 * 
		 * @param time             the {@link ZonedDateTime}
		 * @param gridBuySoftLimit the {@link GridBuySoftLimit}
		 * @param production       the production prediction
		 * @param consumption      the consumption prediction
		 * @param price            the price
		 * @return myself
		 */
		public PeriodsBuilder add(ZonedDateTime time, Integer gridBuySoftLimit, //
				Integer production, Integer consumption, Double price) {
			this.addIfValid(time, gridBuySoftLimit, production, consumption, price);
			return this;
		}

		private static Prediction toPrediction(Integer production, Integer consumption, Environment environment,
				Price price) {
			if (consumption == null) {
				return null;
			}
			final double factor = price == null ? 1. //
					: environment.consumptionFunction.apply(price.normalized());

			return consumption == null ? null //
					: new Prediction(TypeUtils.orElse(production, 0), consumption,
							(int) Math.round(consumption * factor));
		}

		private static Price toPrice(Double value, DoubleToDoubleFunction normalizer) {
			return value == null ? null //
					: new Price(value, normalizer.apply(value));
		}

		public Periods build() {
			if (this.tmpPeriods.isEmpty()) {
				return Periods.empty();
			}

			final var periodLengthHourFromIndex = calculatePeriodDurationHourFromIndex(this.tmpPeriods.getFirst().time);

			final var priceMin = getOrNull(this.tmpPeriods.stream() //
					.map(TmpPeriod::price) //
					.mapToDouble(Double::doubleValue) //
					.min());
			final var priceMax = getOrNull(this.tmpPeriods.stream() //
					.map(TmpPeriod::price) //
					.mapToDouble(Double::doubleValue) //
					.max());
			final DoubleToDoubleFunction normalizePrice = p -> {
				return DoubleUtils.normalize(p, priceMin, priceMax, 0, 1, false);
			};

			final var periods = ImmutableList.<Period>builder();
			var i = 0;
			while (i < this.tmpPeriods.size()) {
				if (i < periodLengthHourFromIndex) {
					// Add QUARTER
					final var tp = this.tmpPeriods.get(i);
					final Period.Quarter p = tp.toPeriodQuarter(this.environment, i, normalizePrice);
					periods.add(p);
					i += 1;

				} else {
					// Add HOUR
					final var qpb = ImmutableList.<Period.Quarter>builder();
					for (var j = 0; j < 4; j++) {
						var index = i + j;
						if (index < this.tmpPeriods.size()) {
							var tp = this.tmpPeriods.get(index);
							qpb.add(tp.toPeriodQuarter(this.environment, j, normalizePrice));
						}
					}
					final var quarterPeriods = qpb.build();

					final var time = quarterPeriods.getFirst().time();
					final var gridBuySoftLimit = quarterPeriods.stream() //
							.map(Period::gridBuySoftLimit) //
							.filter(Objects::nonNull) //
							.reduce(Integer::sum).orElse(null);
					final var production = quarterPeriods.stream() //
							.filter(Period.WithPrediction.class::isInstance) //
							.map(Period.WithPrediction.class::cast) //
							.map(Period.WithPrediction::prediction) //
							.map(Prediction::production) //
							.reduce(Integer::sum).orElse(null);
					final var consumption = quarterPeriods.stream() //
							.filter(Period.WithPrediction.class::isInstance) //
							.map(Period.WithPrediction.class::cast) //
							.map(Period.WithPrediction::prediction) //
							.map(Prediction::consumptionPredicted) //
							.reduce(Integer::sum).orElse(null);
					final var price = toPrice(getOrNull(quarterPeriods.stream() //
							.filter(Period.WithPrice.class::isInstance) //
							.map(Period.WithPrice.class::cast) //
							.map(Period.WithPrice::price) //
							.mapToDouble(Price::actual) //
							.average()), normalizePrice);
					final var prediction = toPrediction(production, consumption, this.environment, price);

					final var j = periodLengthHourFromIndex + (i - periodLengthHourFromIndex) / 4;
					final Period.Hour p;
					if (prediction != null && price != null) {
						p = new Period.Hour.Complete(//
								j, time, gridBuySoftLimit, prediction, price, quarterPeriods);
					} else if (price != null) {
						p = new Period.Hour.WithPrice(//
								j, time, gridBuySoftLimit, price, quarterPeriods);
					} else if (consumption != null) {
						p = new Period.Hour.WithPrediction(//
								j, time, gridBuySoftLimit, prediction, quarterPeriods);
					} else {
						p = new Period.Hour.Empty(//
								j, time, gridBuySoftLimit, quarterPeriods);
					}
					periods.add(p);
					i += 4;
				}
			}

			return new GlobalOptimizationContext.Periods(periods.build());
		}
	}

	public enum PeriodDuration {
		/** Period of duration 15 minutes. */
		QUARTER(Duration.ofMinutes(15)),
		/** Period of duration 1 hour. */
		HOUR(Duration.ofHours(1));

		public final Duration duration;

		/**
		 * Converts power [W] to energy [Wh], considering the duration of the Period.
		 * 
		 * @param power the power value
		 * @return the energy value
		 */
		public int convertPowerToEnergy(int power) {
			return switch (this) {
			case QUARTER -> power / 4;
			case HOUR -> power;
			};
		}

		/**
		 * Converts energy [Wh] to power [W], considering the duration of the Period.
		 * 
		 * @param energy the energy value
		 * @return the power value
		 */
		public int convertEnergyToPower(int energy) {
			return switch (this) {
			case QUARTER -> energy * 4;
			case HOUR -> energy;
			};
		}

		private PeriodDuration(Duration duration) {
			this.duration = duration;
		}
	}

	public static class GocBuilder {
		private final Logger log = LoggerFactory.getLogger(GocBuilder.class);
		private final LogVerbosity logVerbosity;

		private ComponentManager componentManager;
		private Meta meta;
		private Environment environment;
		private ImmutableList<EnergyScheduleHandler> eshs;
		private Sum sum;
		private PredictorManager predictorManager;
		private TimeOfUseTariff timeOfUseTariff;

		protected GocBuilder(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
		}

		private void logInfo(String message) {
			switch (this.logVerbosity) {
			case NONE, DEBUG_LOG -> doNothing();
			case TRACE -> this.log.info("OPTIMIZER " + message);
			}
		}

		private void logWarn(String message) {
			switch (this.logVerbosity) {
			case NONE, DEBUG_LOG -> doNothing();
			case TRACE -> this.log.warn("OPTIMIZER " + message);
			}
		}

		/**
		 * The {@link ComponentManager}.
		 * 
		 * @param componentManager the {@link ComponentManager}
		 * @return myself
		 */
		public GocBuilder setComponentManager(ComponentManager componentManager) {
			this.componentManager = componentManager;
			return this;
		}

		/**
		 * The {@link Meta}.
		 * 
		 * @param meta the {@link Meta}
		 * @return myself
		 */
		public GocBuilder setMeta(Meta meta) {
			this.meta = meta;
			return this;
		}

		/**
		 * The {@link Environment}.
		 * 
		 * @param environment the {@link Environment}
		 * @return myself
		 */
		public GocBuilder setEnvironment(Environment environment) {
			this.environment = environment;
			return this;
		}

		/**
		 * The {@link EnergyScheduleHandler}s of the {@link EnergySchedulable}s.
		 * 
		 * <p>
		 * The list is sorted by Scheduler.
		 * 
		 * @param eshs the list of {@link EnergyScheduleHandler}s
		 * @return myself
		 */
		public GocBuilder setEnergyScheduleHandlers(ImmutableList<EnergyScheduleHandler> eshs) {
			this.eshs = eshs;
			return this;
		}

		/**
		 * The {@link Sum}.
		 * 
		 * @param sum the {@link Sum}
		 * @return myself
		 */
		public GocBuilder setSum(Sum sum) {
			this.sum = sum;
			return this;
		}

		/**
		 * The {@link PredictorManager}.
		 * 
		 * @param predictorManager the {@link PredictorManager}
		 * @return myself
		 */
		public GocBuilder setPredictorManager(PredictorManager predictorManager) {
			this.predictorManager = predictorManager;
			return this;
		}

		/**
		 * The {@link TimeOfUseTariff}.
		 * 
		 * @param timeOfUseTariff the {@link TimeOfUseTariff}
		 * @return myself
		 */
		public GocBuilder setTimeOfUseTariff(TimeOfUseTariff timeOfUseTariff) {
			this.timeOfUseTariff = timeOfUseTariff;
			return this;
		}

		/**
		 * Builds the {@link GlobalOptimizationContext}.
		 * 
		 * @return the {@link GlobalOptimizationContext} record
		 */
		public GlobalOptimizationContext build() throws IllegalArgumentException {
			if (this.componentManager == null) {
				this.logWarn("ComponentManager is not available");
				return null;
			}
			if (this.meta == null) {
				this.logWarn("Meta is not available");
				return null;
			}
			if (this.eshs == null) {
				this.logWarn("EnergyScheduleHandlers are not available");
				return null;
			}
			if (this.sum == null) {
				this.logWarn("Sum is not available");
				return null;
			}
			final var essCapacity = this.sum.getEssCapacity().get();
			if (essCapacity == null) {
				this.logWarn("Sum ESS Capacity is not available");
				return null;
			}
			final var essSoc = this.sum.getEssSoc().get();
			if (essSoc == null) {
				this.logWarn("Sum ESS SoC is not available");
				return null;
			}
			if (this.predictorManager == null) {
				this.logWarn("Predictor-Manager is not available");
				return null;
			}

			final var clock = this.componentManager.getClock();
			final var startTime = roundDownToQuarter(ZonedDateTime.now(clock));

			// Prediction values
			final var consumptions = this.predictorManager.getPrediction(SUM_UNMANAGED_CONSUMPTION);
			this.logInfo("GlobalOptimizationContext CONSUMPTIONS: " + consumptions.toString());
			final var productions = this.predictorManager.getPrediction(SUM_PRODUCTION);
			this.logInfo("GlobalOptimizationContext PRODUCTIONS: " + productions.toString());

			// Prices contains the price values and the time it is retrieved.
			final var prices = this.timeOfUseTariff == null //
					? TimeOfUsePrices.EMPTY_PRICES //
					: this.timeOfUseTariff.getPrices();
			this.logInfo("GlobalOptimizationContext PRICES: " + prices.toString());

			final var endTime = Optional //
					.ofNullable(DateUtils.min(//
							consumptions.getLastTime(), //
							productions.getLastTime(), //
							prices.getLastTime())) //
					.map(i -> i.atZone(clock.getZone())) //
					.orElse(startTime.plusMinutes(SCHEDULE_PERIODS_ON_EMPTY * 15));

			final var gridLimit = this.meta.getMaximumGridFeedInLimitValue().orElse(0);
			final var grid = new Grid(//
					/* maxBuyPower */ gridLimit, //
					/* maxSellPOwer */ gridLimit, //
					/* gridBuySoftLimit */ this.meta.getGridBuySoftLimit());
			final var gridBuySoftLimits = grid.gridBuySoftLimit() //
					.getOneTasksBetween(startTime, endTime.plusMinutes(15));

			final var periodsBuilder = Periods.create(this.environment);
			for (var i = 0;; i++) {
				final var time = startTime.plusMinutes(i * 15);
				final var gridBuySoftLimit = Optional.ofNullable(gridBuySoftLimits.getPayloadAt(time)) //
						.map(GridBuySoftLimit::power) //
						.map(QUARTER::convertPowerToEnergy) //
						.orElse(null);
				final var production = QUARTER.convertPowerToEnergy(//
						productions.getAtOrElse(time, 0 /* defaults to zero */));
				final var consumption = Optional.ofNullable(consumptions.getAt(time)) //
						.map(QUARTER::convertPowerToEnergy) //
						.orElse(null);
				final var price = prices.getAt(time);
				var wasAdded = periodsBuilder.addIfValid(time, gridBuySoftLimit, production, consumption, price);
				if (!wasAdded) {
					break;
				}
			}
			final var periods = periodsBuilder.build();

			if (periods.isEmpty()) {
				this.logWarn("No forecast periods available. " //
						+ "Consumptions[" + consumptions.asArray().length + "] " //
						+ "Productions[" + productions.asArray().length + "] " //
						+ "Prices[" + prices.asArray().length + "]");
				return null;
			}

			final Ess ess;
			{
				var essInitialEnergy = socToEnergy(essCapacity, essSoc);

				// Power Values for scheduling battery for individual periods.
				var maxDischargePower = TypeUtils.max(1000 /* at least 1000 W */, //
						this.sum.getEssMaxDischargePower().get());
				var maxChargePower = TypeUtils.min(-1000 /* at least 1000 W */, //
						this.sum.getEssMinDischargePower().get());

				ess = new Ess(essInitialEnergy, essCapacity, abs(maxChargePower), maxDischargePower);
			}

			final var eshsWithDifferentModes = filterEshsWithDifferentModes(this.eshs) //
					.collect(toImmutableList());
			this.logInfo("OPTIMIZER GlobalOptimizationContext: " //
					+ "startTime=" + startTime + "; " //
					+ "consumptions=" + consumptions.asArray().length + "; " //
					+ "productions=" + productions.asArray().length + "; " //
					+ "prices=" + prices.asArray().length + "; " //
					+ "periods=" + periods.size() + "; " //
					+ "eshs=" + this.eshs.stream() //
							.map(EnergyScheduleHandler::getParentId) //
							.collect(joining(","))
					+ "; " //
					+ "eshsWithDifferentModes=" + eshsWithDifferentModes.stream() //
							.map(EnergyScheduleHandler::getParentId) //
							.collect(joining(",")));

			return new GlobalOptimizationContext(clock, this.environment, startTime, //
					this.eshs, eshsWithDifferentModes, //
					grid, ess, periods);
		}
	}

	/**
	 * Calculates the index when Period duration switches from {@link Duration#HOUR}
	 * to {@link Duration#QUARTER}.
	 * 
	 * <p>
	 * The index is calculated as "6 hours" plus remaining quarters of the current
	 * hour.
	 * 
	 * @param time Start-Timestamp of the Schedule
	 * @return the index
	 */
	// TODO this should be set depending on the actual calculation time and
	// quality of the best schedule result
	protected static int calculatePeriodDurationHourFromIndex(ZonedDateTime time) {
		var minute = time.get(MINUTE_OF_HOUR);
		if (minute == 0) {
			minute = 60;
		}
		return 6 * 4 + (60 - minute) / 15;
	}

	/**
	 * Normalizes the preference ranks of all modes added to optimization for each
	 * ESH in the given {@link GlobalOptimizationContext}.
	 *
	 * <p>
	 * For each ESH in the context, this method:
	 * <ol>
	 * <li>Filters the modes that are considered for optimization (via
	 * {@code addToOptimizer}).</li>
	 * <li>Builds a map from mode index to its raw preference rank.</li>
	 * <li>Normalizes the ranks to a double value between 0.0 and 1.0.</li>
	 * </ol>
	 *
	 * <p>
	 * The result is a list of maps, one per ESH, mapping mode indices to their
	 * normalized preference scores.
	 *
	 * @param eshs the {@link EnergyScheduleHandler.WithDifferentModes}s containing
	 *             their modes
	 * @return a list of maps, each map corresponding to an ESH and mapping mode
	 *         indices to normalized preference ranks between 0.0 and 1.0
	 */
	public static List<Map<Integer, Double>> normalizeEshModePreferenceRanks(
			List<EnergyScheduleHandler.WithDifferentModes> eshs) {
		return eshs.stream()//
				.map(esh -> {
					final Map<Integer, Integer> modeIndexToPreferenceRank = esh.modes().streamAllIndices()//
							.filter(i -> esh.modes().addToOptimizer(i))//
							.boxed()//
							.collect(//
									HashMap::new, //
									(m, i) -> m.put(i, esh.modes().getPreferenceRank(i)), //
									Map::putAll);

					return normalizeModePreferenceRanks(modeIndexToPreferenceRank);
				})//
				.toList();
	}

	@VisibleForTesting
	static Map<Integer, Double> normalizeModePreferenceRanks(Map<Integer, Integer> modeIndexToPreferenceRank) {
		if (modeIndexToPreferenceRank.isEmpty()) {
			return Map.of();
		}

		final Function<Integer, Integer> nullToMax = v -> v != null ? v : Integer.MAX_VALUE;

		final var sortedUniquePreferenceRanks = modeIndexToPreferenceRank.values().stream()//
				.map(nullToMax)//
				.distinct()//
				.sorted()//
				.toList();

		final var preferenceRankToDenseRank = IntStream.range(0, sortedUniquePreferenceRanks.size())//
				.boxed()//
				.collect(Collectors.toMap(//
						sortedUniquePreferenceRanks::get, //
						Function.identity()));

		final int maxDenseRank = Math.max(1, preferenceRankToDenseRank.values().stream()//
				.max(Integer::compareTo)//
				.orElse(0));

		return modeIndexToPreferenceRank.entrySet().stream()//
				.collect(Collectors.toMap(//
						Map.Entry::getKey, //
						e -> preferenceRankToDenseRank.get(nullToMax.apply(e.getValue())) / (double) maxDenseRank));
	}
}
