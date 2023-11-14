package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.jenetics.Optimize.MINIMUM;
import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMinutes;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public class Simulator {

	public static final int NO_OF_PERIODS = 96;
	public static final double EFFICIENCY_FACTOR = 1.2;

	public static record Params(//
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

		protected IntStream forEachIndex() {
			return IntStream.range(0, min(this.productions.length, min(this.consumptions.length, this.prices.length)));
		}

		protected boolean predictionsAreEmpty() {
			return Arrays.stream(this.productions).allMatch(v -> v == 0)
					&& Arrays.stream(this.consumptions).allMatch(v -> v == 0);
		}
	}

	protected static double calculateCost(Params p, StateMachine[] modes) {
		return calculateCost(p, modes, null);
	}

	protected static double calculateCost(Params p, StateMachine[] modes, Consumer<Period> debugPeriod) {
		final var nextEssInitial = new AtomicInteger(p.essAvailableEnergy);
		return p.forEachIndex() //
				.mapToDouble(i -> calculatePeriodCost(p, i, modes, nextEssInitial, debugPeriod)) //
				.sum();
	}

	protected static double calculatePeriodCost(Params p, int i, StateMachine[] modes,
			final AtomicInteger nextEssInitial, Consumer<Period> debugPeriod) {
		final var production = p.productions[i];
		final var consumption = p.consumptions[i];
		final var mode = modes[i];
		final var essInitial = nextEssInitial.get();
		final var essMaxCharge = -min(//
				p.essCapacity - essInitial, // Remaining capacity
				p.essMaxEnergyPerPeriod); // Max per Period
		final var essMaxDischarge = min(//
				essInitial, // Available energy in ESS
				p.essMaxEnergyPerPeriod // Max per Period
		);
		final var price = p.prices[i];
		var essChargeDischarge = switch (mode) {
		case BALANCING ->
			// Apply behaviour of ESS Balancing Controller
			min(max(consumption - production, essMaxCharge), essMaxDischarge);
		case CHARGE ->
			// Charge from grid; max 'maxBuyFromGrid'
			calculateMaxChargePower(p.maxBuyFromGrid, consumption, production, essMaxCharge);
		case DELAY_DISCHARGE -> 0;
		};

		var grid = consumption - production - essChargeDischarge;
		nextEssInitial.set(essInitial - essChargeDischarge);

		final double cost;
		if (grid > 0) {
			// Buy-from-Grid
			var gridConsumption = consumption - production;
			var gridEssCharge = grid - gridConsumption;
			cost = // Cost for direct Consumption
					gridConsumption * price
							// Cost for future Consumption after storage
							+ gridEssCharge * price * EFFICIENCY_FACTOR;
		} else {
			// Sell-to-Grid
			cost = 0.;
		}
		if (debugPeriod != null) {
			var time = p.time.plusMinutes(15 * i);
			debugPeriod.accept(new Period(time, production, consumption, essInitial, essMaxCharge, essMaxDischarge,
					mode, essChargeDischarge, grid, price, cost / 1000000));
		}
		return cost;
	}

	private static int calculateMaxChargePower(int maxBuyFromGrid, int consumption, int production, int essMaxCharge) {
		return max(min(-maxBuyFromGrid + consumption - production, 0), essMaxCharge);
	}

	protected static StateMachine[] getBestSchedule(Params p) {
		// Return pure BALANCING Schedule if no predictions are available
		if (p.predictionsAreEmpty() && Arrays.stream(p.states).anyMatch(m -> m == StateMachine.BALANCING)) {
			System.out.println("Fallback to DEFAULT BALANCING Schedule");
			return IntStream.range(0, NO_OF_PERIODS) //
					.mapToObj(period -> StateMachine.BALANCING) //
					.toArray(StateMachine[]::new);
		}

		var gtf = Genotype.of(IntegerChromosome.of(0, p.states.length), 96);

		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			var modes = IntStream.range(0, 96) //
					.mapToObj(i -> p.states[gt.get(i).get(0).intValue()]) //
					.toArray(StateMachine[]::new);
			return calculateCost(p, modes);
		};
		var engine = Engine //
				.builder(eval, gtf) //
				.executor(Runnable::run) // current thread
				.optimize(MINIMUM) //
				.build();
		var bestGt = engine.stream() //
				.limit(bySteadyFitness(20_000)) //
				.limit(byExecutionTime(ofMinutes(8))) //
				.collect(toBestGenotype());
		return IntStream.range(0, NO_OF_PERIODS) //
				.mapToObj(period -> p.states[bestGt.get(period).get(0).intValue()]) //
				.toArray(StateMachine[]::new);
	}
}
