package io.openems.edge.energy.api.simulation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.filterEshsWithDifferentModes;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration.HOUR;
import static io.openems.edge.energy.api.simulation.GlobalOptimizationContext.PeriodDuration.QUARTER;
import static java.lang.Math.abs;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

import java.time.Clock;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jsonrpc.serialization.JsonSerializer;
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
	public static JsonElement toJson(GlobalOptimizationContext goc) {
		return buildJsonObject() //
				.addProperty("riskLevel", goc.riskLevel) //
				.addProperty("startTime", goc.startTime) //
				.add("grid", Grid.serializer().serialize(goc.grid)) //
				.add("ess", Ess.serializer().serialize(goc.ess)) //
				.add("eshs", goc.eshs.stream() //
						.map(EnergyScheduleHandler::toJson) //
						.collect(toJsonArray())) //
				.build();
	}

	public static record Ess(//
			/** ESS Currently Available Energy (SoC in [Wh]) */
			int currentEnergy, //
			/** ESS Total Energy (Capacity) [Wh] */
			int totalEnergy, //
			/** ESS Max Charge Power [W] */
			int maxChargePower, //
			/** ESS Max Discharge Power [W] */
			int maxDischargePower) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Ess}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Ess> serializer() {
			return jsonObjectSerializer(Ess.class, json -> {
				return new Ess(//
						json.getInt("currentEnergy"), //
						json.getInt("totalEnergy"), //
						json.getInt("maxChargePower"), //
						json.getInt("maxDischargePower"));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("currentEnergy", obj.currentEnergy) //
						.addProperty("totalEnergy", obj.totalEnergy) //
						.addProperty("maxChargePower", obj.maxChargePower) //
						.addProperty("maxDischargePower", obj.maxDischargePower) //
						.build();
			});
		}
	}

	public static record Grid(//
			/** Max Buy-From-Grid Power [W] */
			int maxBuyPower, //
			/** Max Sell-To-Grid Power [W] */
			int maxSellPower) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Grid}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Grid> serializer() {
			return jsonObjectSerializer(Grid.class, json -> {
				return new Grid(//
						json.getInt("maxBuyPower"), //
						json.getInt("maxSellPower"));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("maxBuyPower", obj.maxBuyPower) //
						.addProperty("maxSellPower", obj.maxSellPower) //
						.build();
			});
		}
	}

	public static enum PeriodDuration {
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

	public static sealed interface Period {

		/**
		 * The Duration of the Period.
		 * 
		 * @return the PeriodDuration
		 */
		public PeriodDuration duration();

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

			@Override
			public PeriodDuration duration() {
				return QUARTER;
			}
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

			@Override
			public PeriodDuration duration() {
				return HOUR;
			}
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
				return new Period.Quarter(i, time, //
						QUARTER.convertPowerToEnergy(production), QUARTER.convertPowerToEnergy(consumption), price);
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
				return new Period.Hour(periodLengthHourFromIndex + j, rangeStart, //
						QUARTER.convertPowerToEnergy(production), // ok, because 'sum'
						QUARTER.convertPowerToEnergy(stream(consumption).sum()), //
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

				ess = new Ess(essInitialEnergy, essCapacity, abs(maxChargePower), maxDischargePower);
			}
			final var grid = new Grid(40000 /* TODO */, 20000 /* TODO */);

			final var eshsWithDifferentModes = filterEshsWithDifferentModes(this.eshs) //
					.collect(toImmutableList());
			this.log.info("OPTIMIZER GlobalOptimizationContext: " //
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

			return new GlobalOptimizationContext(clock, this.riskLevel, startTime, //
					this.eshs, eshsWithDifferentModes, //
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
