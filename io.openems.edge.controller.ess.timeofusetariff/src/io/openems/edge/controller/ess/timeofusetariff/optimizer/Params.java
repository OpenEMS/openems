package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static java.lang.Math.min;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.stream.IntStream;

import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public record Params(//
		/** Total number of periods. All arrays have at least this many entries */
		int numberOfPeriods,
		/** Start-Timestamp of the Schedule */
		ZonedDateTime time,
		/** ESS Initially Available Energy (SoC in [Wh]) */
		int essAvailableEnergy, //
		/** ESS Capacity [Wh] */
		int essCapacity, //
		/** ESS Max Charge/Discharge Energy per Period [Wh] */
		int essMaxEnergyPerPeriod, //
		/** Max Buy-From-Grid Energy per Period [Wh] */
		int maxBuyFromGrid,
		/** Production predictions per Period */
		int[] productions, //
		/** Consumption predictions per Period */
		int[] consumptions, //
		/** Prices for one [MWh] per Period */
		float[] prices, //
		/** Allowed Modes */
		StateMachine[] states) {

	public static class Builder {
		private ZonedDateTime time;
		private int essAvailableEnergy;
		private int essCapacity;
		private int essMaxEnergyPerPeriod;
		private int maxBuyFromGrid;
		private int[] productions = new int[0];
		private int[] consumptions = new int[0];
		private float[] prices = new float[0];
		private StateMachine[] states = new StateMachine[0];

		protected Builder time(ZonedDateTime time) {
			this.time = time;
			return this;
		}

		protected Builder essAvailableEnergy(int essAvailableEnergy) {
			this.essAvailableEnergy = essAvailableEnergy;
			return this;
		}

		protected Builder essCapacity(int essCapacity) {
			this.essCapacity = essCapacity;
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

		protected Builder prices(float... prices) {
			this.prices = prices;
			return this;
		}

		protected Builder states(StateMachine... states) {
			this.states = states;
			return this;
		}

		public Params build() {
			var numberOfPeriods = min(this.productions.length, min(this.consumptions.length, this.prices.length));
			return new Params(numberOfPeriods, this.time, this.essAvailableEnergy, this.essCapacity,
					this.essMaxEnergyPerPeriod, this.maxBuyFromGrid, this.productions, this.consumptions, this.prices,
					this.states);
		}
	}

	protected static Builder create() {
		return new Params.Builder();
	}

	protected IntStream forEachIndex() {
		return IntStream.range(0, min(this.productions.length, min(this.consumptions.length, this.prices.length)));
	}

	protected boolean predictionsAreEmpty() {
		return Arrays.stream(this.productions).allMatch(v -> v == 0)
				&& Arrays.stream(this.consumptions).allMatch(v -> v == 0);
	}
}