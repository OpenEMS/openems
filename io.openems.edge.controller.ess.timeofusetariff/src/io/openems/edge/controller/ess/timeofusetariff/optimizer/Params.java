package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateParamsChargeEnergyInChargeGrid;
import static java.lang.Math.min;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.TreeMap;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public record Params(//
		/** Total number of periods. All arrays have at least this many entries */
		int numberOfPeriods,
		/** Start-Timestamp of the Schedule */
		ZonedDateTime time,
		/** ESS Total Energy (Capacity) [Wh] */
		int essTotalEnergy, //
		/** ESS Energy below a configured Minimum-SoC [Wh] */
		int essMinSocEnergy, //
		/** ESS Energy below a configured Maximium-SoC [Wh] */
		int essMaxSocEnergy, //
		/** ESS Initially Available Energy (SoC in [Wh]) */
		int essInitialEnergy, //
		/** ESS Max Charge/Discharge Energy per Period [Wh] */
		int essMaxEnergyPerPeriod, //
		/** ESS Charge Energy per Period in CHARGE_GRID State [Wh] */
		int essChargeInChargeGrid, //
		/** Max Buy-From-Grid Energy per Period [Wh] */
		int maxBuyFromGrid,
		/** Production predictions per Period */
		int[] productions, //
		/** Consumption predictions per Period */
		int[] consumptions, //
		/** Prices for one [MWh] per Period */
		double[] prices, //
		/** Allowed Modes */
		StateMachine[] states, //
		/** The existing Schedule, i.e. result of previous optimization */
		StateMachine[] existingSchedule) {

	public static class Builder {
		private ZonedDateTime time;
		private int essTotalEnergy;
		private int essMinSocEnergy;
		private int essMaxSocEnergy;
		private int essInitialEnergy;
		private int essMaxEnergyPerPeriod;
		private int maxBuyFromGrid;
		private int[] productions = new int[0];
		private int[] consumptions = new int[0];
		private double[] prices = new double[0];
		private StateMachine[] states = new StateMachine[0];
		private StateMachine[] existingSchedule = new StateMachine[0];

		protected Builder time(ZonedDateTime time) {
			this.time = time;
			return this;
		}

		protected Builder essTotalEnergy(int essTotalEnergy) {
			this.essTotalEnergy = essTotalEnergy;
			return this;
		}

		protected Builder essMinSocEnergy(int essMinSocEnergy) {
			this.essMinSocEnergy = essMinSocEnergy;
			return this;
		}

		protected Builder essMaxSocEnergy(int essMaxSocEnergy) {
			this.essMaxSocEnergy = essMaxSocEnergy;
			return this;
		}

		protected Builder essInitialEnergy(int essInitialEnergy) {
			this.essInitialEnergy = essInitialEnergy;
			return this;
		}

		protected Builder essMaxEnergyPerPeriod(int essMaxEnergyPerPeriod) {
			this.essMaxEnergyPerPeriod = essMaxEnergyPerPeriod;
			return this;
		}

		protected Builder maxBuyFromGrid(int maxBuyFromGrid) {
			this.maxBuyFromGrid = maxBuyFromGrid;
			return this;
		}

		protected Builder productions(int... productions) {
			this.productions = productions;
			return this;
		}

		protected Builder consumptions(int... consumptions) {
			this.consumptions = consumptions;
			return this;
		}

		protected Builder prices(double... prices) {
			this.prices = prices;
			return this;
		}

		protected Builder states(StateMachine... states) {
			this.states = states;
			return this;
		}

		protected Builder existingSchedule(TreeMap<ZonedDateTime, Period> existingSchedule) {
			return this.existingSchedule(existingSchedule //
					.tailMap(this.time) // synchronize values with current time
					.values().stream() //
					.map(Period::state) //
					.toArray(StateMachine[]::new));
		}

		protected Builder existingSchedule(StateMachine... existingSchedule) {
			this.existingSchedule = existingSchedule;
			return this;
		}

		public Params build() {
			var numberOfPeriods = min(this.productions.length, min(this.consumptions.length, this.prices.length));
			var essChargeInChargeGrid = calculateParamsChargeEnergyInChargeGrid(this.essMinSocEnergy,
					this.essMaxSocEnergy, this.productions, this.consumptions, this.prices);

			return new Params(numberOfPeriods, //
					this.time, //
					this.essTotalEnergy, this.essMinSocEnergy, this.essMaxSocEnergy, this.essInitialEnergy,
					this.essMaxEnergyPerPeriod, //
					essChargeInChargeGrid, //
					this.maxBuyFromGrid, //
					this.productions, this.consumptions, //
					this.prices, //
					this.states, //
					this.existingSchedule);
		}
	}

	protected static Builder create() {
		return new Params.Builder();
	}

	@Override
	public String toString() {
		return this.toString(true);
	}

	protected String toString(boolean full) {
		StringBuilder b = new StringBuilder();
		b.append("Params [") //
				.append("numberOfPeriods=").append(this.numberOfPeriods) //
				.append(", time=").append(this.time) //
				.append(", essTotalEnergy=").append(this.essTotalEnergy) //
				.append(", essMinSocEnergy=").append(this.essMinSocEnergy) //
				.append(", essMaxSocEnergy=").append(this.essMaxSocEnergy) //
				.append(", essInitialEnergy=").append(this.essInitialEnergy) //
				.append(", essMaxEnergyPerPeriod=").append(this.essMaxEnergyPerPeriod) //
				.append(", essChargeInChargeGrid=").append(this.essChargeInChargeGrid) //
				.append(", maxBuyFromGrid=").append(this.maxBuyFromGrid) //
				.append(", states=").append(Arrays.toString(this.states)); //
		if (full) {
			b //
					.append(", productions=").append(Arrays.toString(this.productions)) //
					.append(", consumptions=").append(Arrays.toString(this.consumptions)) //
					.append(", prices=").append(Arrays.toString(this.prices)) //
					.append(", existingSchedule=").append(Arrays.toString(this.existingSchedule)); //
		}
		return b.append("]").toString();
	}

}