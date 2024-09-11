package io.openems.edge.energy.api.simulation;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.common.type.TypeUtils.assertNull;
import static io.openems.edge.energy.api.EnergyConstants.SUM_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_PRODUCTION;
import static io.openems.edge.energy.api.EnergyConstants.SUM_UNMANAGED_CONSUMPTION;
import static io.openems.edge.energy.api.EnergyUtils.socToEnergy;
import static io.openems.edge.energy.api.EnergyUtils.toEnergy;
import static java.lang.Math.abs;
import static java.lang.Math.min;
import static java.util.Arrays.stream;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Hour;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext.Period.Quarter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

/**
 * Holds the simulation context that is used globally for all simulations in one
 * optimization run.
 * 
 * <p>
 * This record is usually created once per quarter.
 */
public record GlobalSimulationsContext(//
		Clock clock, //
		AtomicInteger simulationCounter, //
		/** Start-Timestamp */
		ZonedDateTime startTime, //
		ImmutableList<EnergyScheduleHandler> handlers, //
		Grid grid, //
		Ess ess, //
		/**
		 * Period is either mixed, with {@link Hour}s and {@link Quarter}s, or
		 * {@link Quarter}s only.
		 */
		ImmutableList<Period> periods) {

	@Override
	public String toString() {
		return new StringBuilder() //
				.append("GlobalSimulationsContext[") //
				.append("startTime=").append(this.startTime).append(", ") //
				.append("grid=").append(this.grid).append(", ") //
				.append("ess=").append(this.ess).append(", ") //
				.append("handlers=").append(this.handlers) //
				.append("]") //
				.toString();
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
	}

	public static record Grid(//
			/** Max Buy-From-Grid Energy [Wh] */
			int maxBuy, //
			/** Max Sell-To-Grid Energy [Wh] */
			int maxSell) {
	}

	public static sealed interface Period {
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
				ZonedDateTime time, //
				int production, //
				int consumption, //
				double price //
		) implements Period {
		}

		public static record Hour(//
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
		private Clock clock;
		private ImmutableList<EnergyScheduleHandler> handlers;
		private Sum sum;
		private PredictorManager predictorManager;
		private TimeOfUseTariff timeOfUseTariff;

		/**
		 * The {@link Clock}.
		 * 
		 * @param clock the {@link Clock}
		 * @return myself
		 */
		public Builder setClock(Clock clock) {
			this.clock = clock;
			return this;
		}

		/**
		 * The {@link EnergyScheduleHandler}s of the {@link EnergySchedulable}s.
		 * 
		 * <p>
		 * The list is sorted by Scheduler.
		 * 
		 * @param handlers the list of {@link EnergyScheduleHandler}s
		 * @return myself
		 */
		public Builder setEnergyScheduleHandlers(ImmutableList<EnergyScheduleHandler> handlers) {
			this.handlers = handlers;
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
		 * Builds the {@link GlobalSimulationsContext}.
		 * 
		 * @return the {@link GlobalSimulationsContext} record
		 */
		public GlobalSimulationsContext build() throws OpenemsException, IllegalArgumentException {
			assertNull("Clock is not available", this.clock);
			assertNull("EnergyScheduleHandlers are not available", this.handlers);
			assertNull("Sum is not available", this.sum);
			assertNull("Predictor-Manager is not available", this.predictorManager);
			assertNull("TimeOfUseTariff is not available", this.timeOfUseTariff);

			final var startTime = DateUtils.roundDownToQuarter(ZonedDateTime.now(this.clock));

			// Prediction values
			final var consumptions = joinConsumptionPredictions(4, //
					this.predictorManager.getPrediction(SUM_CONSUMPTION).asArray(), //
					this.predictorManager.getPrediction(SUM_UNMANAGED_CONSUMPTION).asArray());
			final var productions = generateProductionPrediction(//
					this.predictorManager.getPrediction(SUM_PRODUCTION).asArray(), //
					consumptions.length);

			// Prices contains the price values and the time it is retrieved.
			final var prices = this.timeOfUseTariff.getPrices().asArray();

			final var numberOfPeriods = min(min(consumptions.length, productions.length), prices.length);
			if (numberOfPeriods == 0) {
				throw new IllegalArgumentException("No forecast periods available. " //
						+ "Consumptions[" + consumptions.length + "] " //
						+ "Productions[" + productions.length + "] " //
						+ "Prices[" + prices.length + "]");
			}

			// Helpers
			final IntFunction<Period.Quarter> toQuarterPeriod = (i) -> new Period.Quarter(//
					startTime.plusMinutes(i * 15), //
					toEnergy(productions[i]), //
					toEnergy(consumptions[i]), //
					prices[i]);
			final var periodLengthHourFromIndex = calculatePeriodDurationHourFromIndex(startTime);

			var periods = ImmutableList.<Period>builder();

			// Quarters
			for (var i = 0; i < min(periodLengthHourFromIndex, numberOfPeriods); i++) {
				periods.add(toQuarterPeriod.apply(i));
			}

			// Hours
			final Function<Integer, IntStream> range = (i) -> IntStream.range(i, min(i + 4, numberOfPeriods));
			for (int i = periodLengthHourFromIndex, hour = periodLengthHourFromIndex / 4; //
					i < numberOfPeriods; //
					i += 4, hour++) {
				periods.add(new Period.Hour(//
						startTime.plusHours(hour), //
						toEnergy(range.apply(i).map(j -> productions[j]).sum()), //
						toEnergy(range.apply(i).map(j -> consumptions[j]).sum()), //
						range.apply(i).mapToDouble(j -> prices[j]).average().getAsDouble(), //
						range.apply(i) //
								.mapToObj(j -> toQuarterPeriod.apply(j)) //
								.collect(toImmutableList())));
			}

			final Ess ess;
			{
				var essTotalEnergy = this.sum.getEssCapacity().getOrError();
				var essInitialEnergy = socToEnergy(essTotalEnergy, this.sum.getEssSoc().getOrError());

				// Power Values for scheduling battery for individual periods.
				var maxDischargePower = TypeUtils.max(1000 /* at least 1000 W */, //
						this.sum.getEssMaxDischargePower().get());
				var maxChargePower = TypeUtils.min(-1000 /* at least 1000 W */, //
						this.sum.getEssMinDischargePower().get());
				// TODO
				// if (context.limitChargePowerFor14aEnWG()) {
				// maxChargePower = max(ESS_LIMIT_14A_ENWG, maxChargePower); // Apply §14a EnWG
				// limit
				// }

				ess = new Ess(essInitialEnergy, essTotalEnergy, toEnergy(abs(maxChargePower)),
						toEnergy(maxDischargePower));
			}
			final var grid = new Grid(40000 /* TODO */, 20000 /* TODO */);

			return new GlobalSimulationsContext(this.clock, new AtomicInteger(), startTime, this.handlers, grid, ess,
					periods.build());
		}
	}

	/**
	 * Create a {@link GlobalSimulationsContext} {@link Builder}.
	 * 
	 * @return a {@link Builder}
	 */
	public static Builder create() {
		return new GlobalSimulationsContext.Builder();
	}

	/**
	 * Postprocesses production prediction; makes sure length is at least the same
	 * as consumption prediction - filling up with zeroes.
	 * 
	 * @param prediction the production prediction
	 * @param minLength  the min length (= consumption prediction length)
	 * @return new production prediction
	 */
	protected static Integer[] generateProductionPrediction(Integer[] prediction, int minLength) {
		if (prediction.length >= minLength) {
			return prediction;
		}
		return IntStream.range(0, minLength) //
				.mapToObj(i -> i > prediction.length - 1 ? 0 : prediction[i]) //
				.toArray(Integer[]::new);
	}

	protected static Integer[] joinConsumptionPredictions(int splitAfterIndex, Integer[] totalConsumption,
			Integer[] unmanagedConsumption) {
		return Streams.concat(//
				stream(totalConsumption) //
						.limit(splitAfterIndex), //
				stream(unmanagedConsumption) //
						.skip(splitAfterIndex)) //
				.toArray(Integer[]::new);
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
