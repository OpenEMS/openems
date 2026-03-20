package io.openems.edge.energy.api.simulation;

import static io.openems.common.jsonrpc.serialization.JsonSerializerUtil.jsonObjectSerializer;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration.HOUR;
import static io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration.QUARTER;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.jscalendar.JSCalendar;
import io.openems.common.jsonrpc.serialization.JsonSerializer;
import io.openems.edge.common.meta.GridBuySoftLimit;
import io.openems.edge.energy.api.Environment;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GocUtils.GocBuilder;
import io.openems.edge.energy.api.simulation.GocUtils.PeriodDuration;
import io.openems.edge.energy.api.simulation.GocUtils.PeriodsBuilder;

/**
 * Holds the context that is used globally for an entire optimization run.
 * 
 * <p>
 * This record is usually created once per quarter.
 */
public record GlobalOptimizationContext(//
		Clock clock, //
		Environment environment,
		/** Start-Timestamp */
		ZonedDateTime startTime, //
		ImmutableList<EnergyScheduleHandler> eshs, //
		ImmutableList<EnergyScheduleHandler.WithDifferentModes> eshsWithDifferentModes, //
		Grid grid, //
		Ess ess, //
		Periods periods) {

	/**
	 * Streams the {@link GlobalOptimizationContext.Period.WithPrice}.
	 * 
	 * @return a {@link Stream}
	 */
	public Stream<Period.WithPrice> streamPeriodsWithPrice() {
		return this.periods().stream() //
				.filter(Period.WithPrice.class::isInstance) //
				.map(Period.WithPrice.class::cast);
	}

	/**
	 * Streams the {@link GlobalOptimizationContext.Period.WithPrediction}.
	 * 
	 * @return a {@link Stream}
	 */
	public Stream<Period.WithPrediction> streamPeriodsWithPrediction() {
		return this.periods().stream() //
				.filter(Period.WithPrediction.class::isInstance) //
				.map(Period.WithPrediction.class::cast);
	}

	/**
	 * Streams the {@link GlobalOptimizationContext.Period.Complete}.
	 * 
	 * @return a {@link Stream}
	 */
	public Stream<Period.Complete> streamCompletePeriods() {
		return this.periods().stream() //
				.filter(Period.Complete.class::isInstance) //
				.map(Period.Complete.class::cast);
	}

	/**
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public static JsonElement toJson(GlobalOptimizationContext goc) {
		return buildJsonObject() //
				.addProperty("zone", goc.clock.getZone().getId()) //
				.addProperty("environment", goc.environment) //
				.addProperty("startTime", goc.startTime) //
				.add("grid", goc.grid, Grid.serializer()) //
				.add("ess", goc.ess, Ess.serializer()) //
				.add("eshs", goc.eshs.stream() //
						.map(EnergyScheduleHandler::toJson) //
						.collect(toJsonArray())) //
				.build();
	}

	public static record Grid(//
			/** Max Buy-From-Grid Power [W] */
			int maxBuyPower, //
			/** Max Sell-To-Grid Power [W] */
			int maxSellPower,
			/** The Grid-Buy Soft-Limit [W] */
			JSCalendar.Tasks<GridBuySoftLimit> gridBuySoftLimit) {

		/**
		 * Returns a {@link JsonSerializer} for a {@link Grid}.
		 * 
		 * @return the created {@link JsonSerializer}
		 */
		public static JsonSerializer<Grid> serializer() {
			return jsonObjectSerializer(Grid.class, json -> {
				return new Grid(//
						json.getInt("maxBuyPower"), //
						json.getInt("maxSellPower"), //
						json.getObject("gridBuySoftLimit", GridBuySoftLimit.tasksSerializer()));
			}, obj -> {
				return buildJsonObject() //
						.addProperty("maxBuyPower", obj.maxBuyPower) //
						.addProperty("maxSellPower", obj.maxSellPower) //
						.add("gridBuySoftLimit", obj.gridBuySoftLimit, GridBuySoftLimit.tasksSerializer()) //
						.build();
			});
		}
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

	/**
	 * Create a builder for {@link GlobalOptimizationContext}.
	 * 
	 * @return a {@link GocBuilder}
	 */
	public static GocBuilder create() {
		return new GocBuilder(LogVerbosity.NONE);
	}

	/**
	 * Create a builder for {@link GlobalOptimizationContext}.
	 * 
	 * @param logVerbosity the {@link LogVerbosity}
	 * @return a {@link GocBuilder}
	 */
	public static GocBuilder create(LogVerbosity logVerbosity) {
		return new GocBuilder(logVerbosity);
	}

	/**
	 * Multiple Periods within {@link GlobalOptimizationContext}.
	 */
	public static class Periods {

		private final ImmutableList<Period> periods;

		protected Periods(ImmutableList<Period> periods) {
			this.periods = periods;
		}

		/**
		 * Gets the number of {@link Period Periods}.
		 * 
		 * @return size
		 */
		public int size() {
			return this.periods.size();
		}

		/**
		 * Are there any {@link Period Periods}?.
		 * 
		 * @return true if none
		 */
		public boolean isEmpty() {
			return this.periods.isEmpty();
		}

		/**
		 * Gets a Stream of {@link Period Periods}.
		 * 
		 * @return {@link Stream}
		 */
		public Stream<Period> stream() {
			return this.periods.stream();
		}

		/**
		 * Gets the {@link Period} with given index.
		 * 
		 * @param index the index
		 * @return the {@link Period}
		 */
		public Period get(int index) {
			return this.periods.get(index);
		}

		/**
		 * Gets the first {@link Period}.
		 * 
		 * @return the {@link Period}
		 */
		public Period getFirst() throws NoSuchElementException {
			return this.periods.getFirst();
		}

		/**
		 * Gets the last {@link Period}.
		 * 
		 * @return the {@link Period}
		 */
		public Period getLast() throws NoSuchElementException {
			return this.periods.getLast();
		}

		/**
		 * Create a builder for {@link Periods}.
		 * 
		 * @param environment the {@link Environment}
		 * @return a {@link PeriodsBuilder}
		 */
		public static PeriodsBuilder create(Environment environment) {
			return new PeriodsBuilder(environment);
		}

		/**
		 * Gets object with no {@link Period Periods}.
		 * 
		 * @return empty {@link Periods}
		 */
		public static Periods empty() {
			return new Periods(ImmutableList.of());
		}

		/**
		 * Copies the Quarterly Periods of the given {@link Periods} to a new Periods.
		 * 
		 * @param o the given Periods
		 * @return copy
		 */
		public static Periods copyOfQuarterly(Periods o) {
			return new Periods(o.stream() //
					.flatMap(period -> switch (period) {
					case GlobalOptimizationContext.Period.Hour ph //
						-> ph.quarterPeriods().stream();
					case GlobalOptimizationContext.Period.Quarter pq //
						-> Stream.of(pq);
					}) //
					.collect(ImmutableList.<Period>toImmutableList()));
		}
	}

	/**
	 * One single Period of {@link Periods}.
	 */
	public sealed interface Period {

		/**
		 * The Duration of a Period.
		 * 
		 * @return the {@link PeriodDuration}
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
		 * The Grid-Buy Soft-Limit in [Wh].
		 * 
		 * @return {@link GridBuySoftLimit}; or null
		 */
		public Integer gridBuySoftLimit();

		public static sealed interface Empty extends Period {
		}

		public static record Prediction(//
				/**
				 * Production prediction for the Period in [Wh].
				 * 
				 * @return the production prediction
				 */
				int production,

				/**
				 * Consumption prediction for the Period in [Wh].
				 * 
				 * @return the consumption prediction
				 */
				int consumptionPredicted,

				/**
				 * Consumption prediction for the Period adjusted by {@link Environment} in [Wh].
				 * 
				 * @return the consumption prediction
				 */
				int consumptionRiskAdjusted) {

			/**
			 * Gets the excess Consumption, i.e. Consumption minus Production.
			 * 
			 * @return value
			 */
			public int excessConsumption() {
				return this.consumptionPredicted - this.production;
			}

			/**
			 * Gets the excess Production, i.e. Production minus Consumption.
			 * 
			 * @return value
			 */
			public int excessProduction() {
				return this.production - this.consumptionPredicted;
			}
		}

		public static sealed interface WithPrediction extends Period {
			/**
			 * Prediction for the Period in [Wh].
			 * 
			 * @return the {@link Price}
			 */
			public Prediction prediction();
		}

		public static record Price(//
				/**
				 * Actual (Average) Grid-Buy-Price for the Period in [1/MWh].
				 * 
				 * @return the actual price
				 */
				double actual,

				/**
				 * Normalized Grid-Buy-Price for the Period in range [0; 1].
				 * 
				 * @return the normalized price
				 */
				double normalized) {
		}

		public static sealed interface WithPrice extends Period {
			/**
			 * Grid-Buy-Price for the Period in [1/MWh].
			 * 
			 * @return the {@link Price}
			 */
			public Price price();
		}

		public static sealed interface Complete extends Period.WithPrediction, Period.WithPrice {
		}

		public static sealed interface Quarter extends Period {

			@Override
			public default PeriodDuration duration() {
				return QUARTER;
			}

			public static record Empty(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit //
			) implements Period.Quarter, Period.Empty {
			}

			public static record WithPrediction(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrediction
					Prediction prediction //
			) implements Period.Quarter, Period.WithPrediction {
			}

			public static record WithPrice(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrice
					Price price //
			) implements Period.Quarter, Period.WithPrice {
			}

			public static record Complete(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrediction
					Prediction prediction, //
					// From Period.WithPrice
					Price price //
			) implements Period.Quarter, Period.Complete {
			}
		}

		public static sealed interface Hour extends Period {

			@Override
			public default PeriodDuration duration() {
				return HOUR;
			}

			/**
			 * Raw Periods, representing one QUARTER.
			 * 
			 * @return the Quarter Periods
			 */
			public ImmutableList<Period.Quarter> quarterPeriods();

			public static record Empty(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.Hour
					ImmutableList<Period.Quarter> quarterPeriods //
			) implements Period.Hour, Period.Empty {
			}

			public static record WithPrediction(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrediction
					Prediction prediction, //
					// From Period.Hour
					ImmutableList<Period.Quarter> quarterPeriods //
			) implements Period.Hour, Period.WithPrediction {
			}

			public static record WithPrice(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrice
					Price price, //
					// From Period.Hour
					ImmutableList<Period.Quarter> quarterPeriods //
			) implements Period.Hour, Period.WithPrice {
			}

			public static record Complete(
					// From Period
					int index, ZonedDateTime time, Integer gridBuySoftLimit, //
					// From Period.WithPrediction
					Prediction prediction, //
					// From Period.WithPrice
					Price price, //
					// From Period.Hour
					ImmutableList<Period.Quarter> quarterPeriods //
			) implements Period.Hour, Period.Complete {
			}
		}
	}
}
