package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.bySteadyFitness;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateStateChargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessPeriodState;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMinutes;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public class Simulator {

	/** Used to incorporate charge/discharge efficiency. */
	public static final double EFFICIENCY_FACTOR = 1.2;

	protected static double calculateCost(Params p, StateMachine[] states) {
		return calculateCost(p, states, null);
	}

	protected static double calculateCost(Params p, StateMachine[] states, Consumer<Period> collect) {
		final var nextEssInitial = new AtomicInteger(p.essAvailableEnergy());
		var sum = 0.;
		for (var i = 0; i < p.numberOfPeriods(); i++) {
			sum += calculatePeriodCost(p, i, states, nextEssInitial, collect);
		}
		return sum;
	}

	protected static double calculatePeriodCost(Params p, int i, StateMachine[] states,
			final AtomicInteger nextEssInitial, Consumer<Period> collect) {
		final var production = p.productions()[i];
		final var consumption = p.consumptions()[i];
		final var state = states[i];
		final var essInitial = nextEssInitial.get();
		final var essMaxCharge = -min(//
				p.essCapacity() - essInitial, // Remaining capacity
				p.essMaxEnergyPerPeriod()); // Max per Period
		final var essMaxDischarge = min(//
				essInitial, // Available energy in ESS
				p.essMaxEnergyPerPeriod() // Max per Period
		);
		final var price = p.prices()[i];
		final var balancingChargeDischarge = min(max(consumption - production, essMaxCharge), essMaxDischarge);
		var essChargeDischarge = switch (state) {
		case BALANCING ->
			// Apply behaviour of ESS Balancing Controller
			balancingChargeDischarge;
		case CHARGE ->
			// Charge from grid; max 'maxBuyFromGrid'
			calculateStateChargeEnergy(p.maxBuyFromGrid(), consumption, production, essMaxCharge);
		case DELAY_DISCHARGE -> 0;
		};

		// Calculate Grid energy
		var grid = consumption - production - essChargeDischarge;
		nextEssInitial.set(essInitial - essChargeDischarge);
		var gridConsumption = consumption - production //
				- max(essChargeDischarge, 0) /* consider only ESS discharge, i.e. positive values */;
		var gridEssCharge = grid - gridConsumption;

		double cost;
		if (grid > 0) {
			cost = // Cost for direct Consumption
					gridConsumption * price
							// Cost for future Consumption after storage
							+ gridEssCharge * price * EFFICIENCY_FACTOR;

		} else {
			// Sell-to-Grid
			cost = 0.;
		}
		if (collect != null) {
			var time = p.time().plusMinutes(15 * i);
			var postprocessedState = postprocessPeriodState(p, essInitial, gridEssCharge, price,
					balancingChargeDischarge, state);
			collect.accept(new Period(time, production, consumption, essInitial, essMaxCharge, essMaxDischarge,
					postprocessedState, essChargeDischarge, grid, price, cost / 1000000));
		}
		return cost;
	}

	/**
	 * Runs the optimization with default settings.
	 * 
	 * @param p the {@link Params}
	 * @return the best schedule
	 */
	protected static StateMachine[] getBestSchedule(Params p) {
		return getBestSchedule(p, null, null);
	}

	protected static StateMachine[] getBestSchedule(Params p, Integer populationSize, Integer limit) {
		// Return pure BALANCING Schedule if no predictions are available
		if (p.numberOfPeriods() == 0 || p.predictionsAreEmpty()) {
			System.out.println("Fallback to DEFAULT BALANCING Schedule");
			return IntStream.range(0, p.numberOfPeriods()) //
					.mapToObj(period -> StateMachine.BALANCING) //
					.toArray(StateMachine[]::new);
		}

		var gtf = Genotype.of(IntegerChromosome.of(IntegerGene.of(0, p.states().length)), p.numberOfPeriods()); //
		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			var modes = new StateMachine[p.numberOfPeriods()];
			for (var i = 0; i < modes.length; i++) {
				modes[i] = p.states()[gt.get(i).get(0).intValue()];
			}
			return calculateCost(p, modes);
		};
		var engine = Engine //
				.builder(eval, gtf) //
				.executor(Runnable::run) // current thread
				.minimizing();
		if (populationSize != null) {
			engine.populationSize(populationSize); //
		}
		Stream<EvolutionResult<IntegerGene, Double>> stream = engine.build() //
				.stream(buildInitialPopulation(p)) //
				.limit(bySteadyFitness(20_000)) //
				.limit(byExecutionTime(ofMinutes(11))); //
		if (limit != null) {
			stream = stream.limit(limit); // apply optional limit
		}
		var bestGt = stream //
				.collect(toBestGenotype());
		return IntStream.range(0, p.numberOfPeriods()) //
				.mapToObj(period -> p.states()[bestGt.get(period).get(0).intValue()]) //
				.toArray(StateMachine[]::new);
	}
}
