package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.openems.edge.energy.optimizer.ParamsUtils.calculateChargeEnergyInChargeGrid;
import static io.openems.edge.energy.optimizer.ParamsUtils.calculatePeriodLengthHourFromIndex;
import static java.lang.Math.min;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public record Params(//
		/** Start-Timestamp of the Schedule */
		ZonedDateTime time,
		/** ESS Total Energy (Capacity) [Wh] */
		int essTotalEnergy, //
		/** ESS Energy below a configured Minimum-SoC [Wh] */
		int essMinSocEnergy, //
		/** ESS Energy below a configured Maximum-SoC [Wh] */
		int essMaxSocEnergy, //
		/** ESS Initially Available Energy (SoC in [Wh]) */
		int essInitialEnergy, //
		/** Allowed Modes */
		StateMachine[] states, //
		/** The existing Schedule, i.e. result of previous optimization */
		ImmutableSortedMap<ZonedDateTime, StateMachine> existingSchedule, //
		/** Periods for the Optimizer, representing QUARTER or HOUR. */
		ImmutableList<OptimizePeriod> optimizePeriods //
) {
	public static enum Length {
		HOUR, QUARTER
	}

	public static record OptimizePeriod(//
			/** Start-Timestamp of the Period */
			ZonedDateTime time, //
			/** Length of the Period */
			Length length,
			/** ESS Max Charge Energy [Wh] */
			int essMaxChargeEnergy, //
			/** ESS Max Discharge Energy [Wh] */
			int essMaxDischargeEnergy, //
			/** ESS Charge Energy in CHARGE_GRID State [Wh] */
			int essChargeInChargeGrid, //
			/** Max Buy-From-Grid Energy [Wh] */
			int maxBuyFromGrid,
			/** Production prediction */
			int production, //
			/** Consumption prediction */
			int consumption, //
			/** Price [1/MWh] */
			double price, //
			/** Raw Periods, representing one QUARTER. */
			ImmutableList<QuarterPeriod> quarterPeriods //
	) {
	}

	public static record QuarterPeriod(//
			/** Start-Timestamp of the Period */
			ZonedDateTime time,
			/** ESS Max Charge Energy [Wh] */
			int essMaxChargeEnergy, //
			/** ESS Max Discharge Energy [Wh] */
			int essMaxDischargeEnergy, //
			/** ESS Charge Energy in CHARGE_GRID State [Wh] */
			int essChargeInChargeGrid, //
			/** Max Buy-From-Grid Energy [Wh] */
			int maxBuyFromGrid,
			/** Production prediction */
			int production, //
			/** Consumption predictions */
			int consumption, //
			/** Price [1/MWh] */
			double price //
	) {
	}

	public static class Builder {
		private ZonedDateTime time;
		private int essTotalEnergy;
		private int essMinSocEnergy;
		private int essMaxSocEnergy;
		private int essInitialEnergy;
		private int essMaxChargeEnergy;
		private int essMaxDischargeEnergy;
		private int maxBuyFromGrid;
		private int[] productions = new int[0];
		private int[] consumptions = new int[0];
		private double[] prices = new double[0];
		private StateMachine[] states = new StateMachine[0];
		private ImmutableSortedMap<ZonedDateTime, StateMachine> existingSchedule;

		protected Builder setTime(ZonedDateTime time) {
			this.time = time;
			return this;
		}

		protected Builder setEssTotalEnergy(int essTotalEnergy) {
			this.essTotalEnergy = essTotalEnergy;
			return this;
		}

		protected Builder setEssMinSocEnergy(int essMinSocEnergy) {
			this.essMinSocEnergy = essMinSocEnergy;
			return this;
		}

		protected Builder setEssMaxSocEnergy(int essMaxSocEnergy) {
			this.essMaxSocEnergy = essMaxSocEnergy;
			return this;
		}

		protected Builder setEssInitialEnergy(int essInitialEnergy) {
			this.essInitialEnergy = essInitialEnergy;
			return this;
		}

		protected Builder setEssMaxChargeEnergy(int essMaxChargeEnergy) {
			this.essMaxChargeEnergy = essMaxChargeEnergy;
			return this;
		}

		protected Builder setEssMaxDischargeEnergy(int essMaxDischargeEnergy) {
			this.essMaxDischargeEnergy = essMaxDischargeEnergy;
			return this;
		}

		protected Builder seMaxBuyFromGrid(int maxBuyFromGrid) {
			this.maxBuyFromGrid = maxBuyFromGrid;
			return this;
		}

		protected Builder setProductions(int... productions) {
			this.productions = productions;
			return this;
		}

		protected Builder setConsumptions(int... consumptions) {
			this.consumptions = consumptions;
			return this;
		}

		protected Builder setPrices(double... prices) {
			this.prices = prices;
			return this;
		}

		protected Builder setStates(StateMachine... states) {
			this.states = states;
			return this;
		}

		protected Builder setExistingSchedule(ImmutableSortedMap<ZonedDateTime, StateMachine> existingSchedule) {
			this.existingSchedule = existingSchedule.tailMap(this.time);
			return this;
		}

		private ImmutableList<OptimizePeriod> generatePeriods() {
			var essChargeInChargeGrid = calculateChargeEnergyInChargeGrid(this.essMinSocEnergy, this.essMaxSocEnergy,
					this.productions, this.consumptions, this.prices);
			var noOfPeriods = min(this.productions.length, min(this.consumptions.length, this.prices.length));

			final Function<Integer, QuarterPeriod> toQuarterPeriod = (i) -> new QuarterPeriod(
					this.time.plusMinutes(i * 15), this.essMaxChargeEnergy, this.essMaxDischargeEnergy,
					essChargeInChargeGrid, this.maxBuyFromGrid, this.productions[i], this.consumptions[i],
					this.prices[i]);
			final Function<Integer, Integer> count = (i) -> min(i + 4, noOfPeriods) - i;
			final Function<Integer, IntStream> range = (i) -> IntStream.range(i, min(i + 4, noOfPeriods));

			var periodLengthHourFromIndex = calculatePeriodLengthHourFromIndex(this.time);
			var result = ImmutableList.<OptimizePeriod>builder();

			// Quarters
			for (var i = 0; i < min(periodLengthHourFromIndex, noOfPeriods); i++) {
				result.add(new OptimizePeriod(//
						this.time.plusMinutes(i * 15), Length.QUARTER, this.essMaxChargeEnergy,
						this.essMaxDischargeEnergy, essChargeInChargeGrid, this.maxBuyFromGrid, this.productions[i],
						this.consumptions[i], this.prices[i], //
						ImmutableList.of(toQuarterPeriod.apply(i))));
			}

			// Hours
			for (var i = periodLengthHourFromIndex; i < noOfPeriods; i += 4) {
				var factor = count.apply(i);
				result.add(new OptimizePeriod(//
						this.time.plusHours(i), //
						Length.HOUR, //
						factor * this.essMaxChargeEnergy, //
						factor * this.essMaxDischargeEnergy, //
						factor * essChargeInChargeGrid, //
						factor * this.maxBuyFromGrid, //
						range.apply(i).map(j -> this.productions[j]).sum(),
						range.apply(i).map(j -> this.consumptions[j]).sum(),
						range.apply(i).mapToDouble(j -> this.prices[j]).average().getAsDouble(), //
						range.apply(i) //
								.mapToObj(j -> toQuarterPeriod.apply(j)) //
								.collect(toImmutableList())));
			}
			return result.build();
		}

		public Params build() {
			return new Params(this.time, this.essTotalEnergy, this.essMinSocEnergy, this.essMaxSocEnergy,
					this.essInitialEnergy, this.states, //
					this.existingSchedule, this.generatePeriods());
		}
	}

	protected static Builder create() {
		return new Params.Builder();
	}

	@Override
	public String toString() {
		return this.toLogString();
	}

	public static final Pattern PARAMS_PATTERN = Pattern.compile("^" //
			+ ".*time=(?<time>\\S+)," //
			+ ".*essTotalEnergy=(?<essTotalEnergy>\\d+)" //
			+ ".*essMinSocEnergy=(?<essMinSocEnergy>\\d+)" //
			+ ".*essMaxSocEnergy=(?<essMaxSocEnergy>\\d+)" //
			+ ".*essInitialEnergy=(?<essInitialEnergy>\\d+)" //
			+ ".*states=\\[(?<states>[A-Z_, ]+)\\]" //
			+ ".*$");

	protected String toLogString() {
		return new StringBuilder() //
				.append("Params") //
				.append(" [time=").append(this.time) //
				.append(", essTotalEnergy=").append(this.essTotalEnergy) //
				.append(", essMinSocEnergy=").append(this.essMinSocEnergy) //
				.append(", essMaxSocEnergy=").append(this.essMaxSocEnergy) //
				.append(", essInitialEnergy=").append(this.essInitialEnergy) //
				.append(", states=").append(Arrays.toString(this.states)) //
				.append("]") //
				.toString();
	}

}