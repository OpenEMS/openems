package io.openems.edge.energy.api.simulation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.getAsInt;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.RiskLevel;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period.Quarter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Holds the context that is used globally for an entire optimization run.
 * 
 * <p>
 * This record is usually created once per quarter.
 */
public record GlobalOptimizationContext(//
		Clock clock, //
		RiskLevel riskLevel,
		/** Start-Timestamp */
		ZonedDateTime startTime, //
		ImmutableList<EnergyScheduleHandler> eshs, //
		ImmutableList<EnergyScheduleHandler.WithDifferentModes> eshsWithDifferentModes, //
		Grid grid, //
		Ess ess, //
		/**
		 * Period is either mixed, with {@link Hour}s and {@link Quarter}s, or
		 * {@link Quarter}s only.
		 */
		ImmutableList<Period> periods) {

	/**
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson() {
		return buildJsonObject() //
				.addProperty("startTime", this.startTime) //
				.add("grid", this.grid.toJson()) //
				.add("ess", this.ess.toJson()) //
				.add("eshs", this.eshs.stream() //
						.map(EnergyScheduleHandler::toJson) //
						.collect(toJsonArray())) //
				.build();
	}

	public static record Ess(//
			/** ESS Currently Available Energy (SoC in [Wh]) */
			int currentEnergy, //
			/** ESS Total Energy (Capacity) [Wh] */
			int totalEnergy, //
			/** ESS Max Charge Energy [Wh] */
			int maxChargeEnergy, //
			/** ESS Max Discharge Energy [Wh] */
			int maxDischargeEnergy) {

		/**
		 * Serialize.
		 * 
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJson() {
			return buildJsonObject() //
					.addProperty("currentEnergy", this.currentEnergy) //
					.addProperty("totalEnergy", this.totalEnergy) //
					.addProperty("maxChargeEnergy", this.maxChargeEnergy) //
					.addProperty("maxDischargeEnergy", this.maxDischargeEnergy) //
					.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonObject}
		 * @return the {@link Ess}
		 * @throws OpenemsNamedException on error
		 */
		public static Ess fromJson(JsonObject j) throws OpenemsNamedException {
			return new Ess(//
					getAsInt(j, "currentEnergy"), //
					getAsInt(j, "totalEnergy"), //
					getAsInt(j, "maxChargeEnergy"), //
					getAsInt(j, "maxDischargeEnergy"));
		}
	}

	public static record Grid(//
			/** Max Buy-From-Grid Energy [Wh] */
			int maxBuy, //
			/** Max Sell-To-Grid Energy [Wh] */
			int maxSell) {

		/**
		 * Serialize.
		 * 
		 * @return the {@link JsonObject}
		 */
		public JsonObject toJson() {
			return buildJsonObject() //
					.addProperty("maxBuy", this.maxBuy) //
					.addProperty("maxSell", this.maxSell) //
					.build();
		}

		/**
		 * Deserialize.
		 * 
		 * @param j a {@link JsonObject}
		 * @return the {@link Grid}
		 * @throws OpenemsNamedException on error
		 */
		public static Grid fromJson(JsonObject j) throws OpenemsNamedException {
			return new Grid(//
					getAsInt(j, "maxBuy"), //
					getAsInt(j, "maxSell"));
		}
	}

	public static sealed interface Period {
		/**
		 * Index of the Period.
		 * 
		 * @return the index
		 */
		public int index();

		/**
		 * Start-Timestamp of the Period.
		 * 
		 * @return the {@link ZonedDateTime}
		 */
		public ZonedDateTime time();

		/**
		 * Production prediction for the Period in [Wh].
		 * 
		 * @return the production prediction
		 */
		public int production();

		/**
		 * Consumption prediction for the Period in [Wh].
		 * 
		 * @return the consumption prediction
		 */
		public int consumption();

		/**
		 * (Average) Grid-Buy-Price for the Period in [1/MWh].
		 * 
		 * @return the price
		 */
		public double price();

		public static record Quarter(//
				int index, //
				ZonedDateTime time, //
				int production, //
				int consumption, //
				double price //
		) implements Period {
		}

		public static record Hour(//
				int index, //
				ZonedDateTime time, //
				int production, //
				int consumption, //
				double price, //
				/** Raw Periods, representing one QUARTER. */
				ImmutableList<Period.Quarter> quarterPeriods //
		) implements Period {
		}
	}

	public static class Builder {
		private final Logger log = LoggerFactory.getLogger(Builder.class);
		private final LogVerbosity logVerbosity;

		private ComponentManager componentManager;
		private RiskLevel riskLevel;
		private ImmutableList<EnergyScheduleHandler> eshs;
		private Sum sum;
		private PredictorManager predictorManager;
		private TimeOfUseTariff timeOfUseTariff;

		protected Builder(LogVerbosity logVerbosity) {
			this.logVerbosity = logVerbosity;
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
		public Builder setComponentManager(ComponentManager componentManager) {
			this.componentManager = componentManager;
			return this;
		}

		/**
		 * The {@link RiskLevel}.
		 * 
		 * @param riskLevel the {@link RiskLevel}
		 * @return myself
		 */
		public Builder setRiskLevel(RiskLevel riskLevel) {
			this.riskLevel = riskLevel;
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
		public Builder setEnergyScheduleHandlers(ImmutableList<EnergyScheduleHandler> eshs) {
			this.eshs = eshs;
			return this;
		}

		/**
		 * The {@link Sum}.
		 * 
		 * @param sum the {@link Sum}
		 * @return myself
		 */
		public Builder setSum(Sum sum) {
			this.sum = sum;
			return this;
		}

		/**
		 * The {@link PredictorManager}.
		 * 
		 * @param predictorManager the {@link PredictorManager}
		 * @return myself
		 */
		public Builder setPredictorManager(PredictorManager predictorManager) {
			this.predictorManager = predictorManager;
			return this;
		}

		/**
		 * The {@link TimeOfUseTariff}.
		 * 
		 * @param timeOfUseTariff the {@link TimeOfUseTariff}
		 * @return myself
		 */
		public Builder setTimeOfUseTariff(TimeOfUseTariff timeOfUseTariff) {
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
			if (this.timeOfUseTariff == null) {
				this.logWarn("TimeOfUseTariff is not available");
				return null;
			}
			final var clock = this.componentManager.getClock();
			final var startTime = DateUtils.roundDownToQuarter(ZonedDateTime.now(clock));
			final var periodLengthHourFromIndex = calculatePeriodDurationHourFromIndex(startTime);

			// Prediction values
			final var consumptions = this.predictorManager.getPrediction(SUM_UNMANAGED_CONSUMPTION);
			final var productions = this.predictorManager.getPrediction(SUM_PRODUCTION);

			// Prices contains the price values and the time it is retrieved.
			final var prices = this.timeOfUseTariff.getPrices();

			// Helpers
			final IntFunction<Period.Quarter> toQuarterPeriod = (i) -> {
				final var time = startTime.plusMinutes(i * 15);
				final var consumption = consumptions.getAt(time);
				final var price = prices.getAt(time);
				if (consumption == null || price == null) {
					return null;
				}
				final var production = productions.getAtOrElse(time, 0);
				return new Period.Quarter(i, time, toEnergy(production), toEnergy(consumption), price);
			};
			final IntFunction<Period.Hour> toHourPeriod = (j) -> {
				final var i = periodLengthHourFromIndex + j * 4;
				final var rangeStart = startTime.plusMinutes(i * 15);
				final var rangeEnd = rangeStart.plusMinutes(60);

				final var consumption = consumptions //
						.getBetween(rangeStart, rangeEnd) //
						.mapToInt(Integer::intValue) //
						.toArray();
				final var priceRange = prices //
						.getBetween(rangeStart, rangeEnd) //
						.mapToDouble(Double::doubleValue) //
						.toArray();
				if (consumption.length == 0 || priceRange.length == 0) {
					return null;
				}
				final var price = stream(priceRange).average().getAsDouble();
				final var production = productions //
						.getBetween(rangeStart, rangeEnd) //
						.mapToInt(Integer::intValue) //
						.sum();
				final var quarterPeriods = IntStream.range(i, i + 4) //
						.mapToObj(toQuarterPeriod) //
						.filter(Objects::nonNull) //
						.collect(toImmutableList());
				// TODO toEnergy per Hour?
				return new Period.Hour(periodLengthHourFromIndex + j, rangeStart, //
						toEnergy(production), toEnergy(stream(consumption).sum()), //
						price, quarterPeriods);
			};

			var periods = Stream.concat(//
					IntStream.range(0, periodLengthHourFromIndex) //
							.<Period>mapToObj(toQuarterPeriod), //
					IntStream.iterate(0, i -> i + 1) //
							.<Period>mapToObj(toHourPeriod) //
							.takeWhile(Objects::nonNull)) //
					.filter(Objects::nonNull) //
					.collect(toImmutableList());

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

				ess = new Ess(essInitialEnergy, essCapacity, toEnergy(abs(maxChargePower)),
						toEnergy(maxDischargePower));
			}
			final var grid = new Grid(40000 /* TODO */, 20000 /* TODO */);

			this.log.info("OPTIMIZER GlobalOptimizationContext: " //
					+ "startTime=" + startTime + "; " //
					+ "consumptions=" + consumptions.asArray().length + "; " //
					+ "productions=" + productions.asArray().length + "; " //
					+ "prices=" + prices.asArray().length + "; " //
					+ "periods=" + periods.size());

			return new GlobalOptimizationContext(clock, this.riskLevel, startTime, //
					this.eshs, filterEshsWithDifferentModes(this.eshs).collect(toImmutableList()), //
					grid, ess, periods);
		}
	}

	/**
	 * Create a {@link GlobalOptimizationContext} {@link Builder}.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new GlobalOptimizationContext.Builder(LogVerbosity.NONE);
	}

	/**
	 * Create a {@link GlobalOptimizationContext} {@link Builder}.
	 * 
	 * @param logVerbosity the {@link LogVerbosity}
	 * @return a {@link Builder}
	 */
	public static Builder create(LogVerbosity logVerbosity) {
		return new GlobalOptimizationContext.Builder(logVerbosity);
	}

	/**
	 * Calculates the index when Period duration switches from {@link Hour} to
	 * {@link Quarter}.
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
		var minute = time.getMinute();
		if (minute == 0) {
			minute = 60;
		}
		return 6 * 4 + (60 - minute) / 15;
	}
}
