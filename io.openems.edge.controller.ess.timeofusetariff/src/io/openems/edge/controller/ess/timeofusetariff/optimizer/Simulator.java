package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.bySteadyFitness;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofMinutes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.jenetics.Chromosome;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;

public class Simulator {

	/** Used to incorporate charge/discharge efficiency. */
	public static final double EFFICIENCY_FACTOR = 1.2;

	/** Used to penalize bad solutions. */
	public static final double PENALIZE_FACTOR = 10;

	protected static double calculateCost(Params p, StateMachine[] modes) {
		return calculateCost(p, modes, null);
	}

	protected static double calculateCost(Params p, StateMachine[] modes, Consumer<Period> debugPeriod) {
		final var nextEssInitial = new AtomicInteger(p.essAvailableEnergy());
		return p.forEachIndex() //
				.mapToDouble(i -> calculatePeriodCost(p, i, modes, nextEssInitial, debugPeriod)) //
				.sum();
	}

	protected static double calculatePeriodCost(Params p, int i, StateMachine[] modes,
			final AtomicInteger nextEssInitial, Consumer<Period> debugPeriod) {
		final var production = p.productions()[i];
		final var consumption = p.consumptions()[i];
		final var mode = modes[i];
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
		var essChargeDischarge = switch (mode) {
		case BALANCING ->
			// Apply behaviour of ESS Balancing Controller
			balancingChargeDischarge;
		case CHARGE ->
			// Charge from grid; max 'maxBuyFromGrid'
			calculateMaxChargePower(p.maxBuyFromGrid(), consumption, production, essMaxCharge);
		case DELAY_DISCHARGE -> 0;
		};

		var grid = consumption - production - essChargeDischarge;
		nextEssInitial.set(essInitial - essChargeDischarge);

		double cost;
		if (grid > 0) {
			// Buy-from-Grid
			var gridConsumption = consumption - production;
			var gridEssCharge = grid - gridConsumption;
			cost = // Cost for direct Consumption
					gridConsumption * price
							// Cost for future Consumption after storage
							+ gridEssCharge * price * EFFICIENCY_FACTOR;

			// Penalize bad solutions
			if (isBadSolution(p, mode, essChargeDischarge, gridEssCharge, essInitial)) {
				cost *= PENALIZE_FACTOR;
			}
		} else {
			// Sell-to-Grid
			cost = 0.;
		}
		if (debugPeriod != null) {
			var time = p.time().plusMinutes(15 * i);
			debugPeriod.accept(new Period(time, production, consumption, essInitial, essMaxCharge, essMaxDischarge,
					mode, essChargeDischarge, grid, price, cost / 1000000));
		}
		return cost;
	}

	private static int calculateMaxChargePower(int maxBuyFromGrid, int consumption, int production, int essMaxCharge) {
		return max(min(-maxBuyFromGrid + consumption - production, 0), essMaxCharge);
	}

	// TODO implement this as Constraint
	protected static boolean isBadSolution(Params p, StateMachine mode, int balancingChargeDischarge, int gridEssCharge,
			int essInitial) {
		var soc = essInitial * 100 / p.essCapacity();
		return switch (mode) {
		case BALANCING -> false;

		case CHARGE -> {
			// CHARGE,...
			if (gridEssCharge <= 0) {
				// but actually charging from PV -> could have been BALANCING
				yield true;
			} else if (soc >= 90) {
				// but battery was already > 90 % SoC -> too risky
				yield true;
			}
			yield false;
		}

		case DELAY_DISCHARGE -> {
			// DELAY_DISCHARGE,...
			if (balancingChargeDischarge < 0) {
				// but actually charging from PV -> could have been BALANCING
				yield true;
			} else if (soc <= 10) {
				// but battery is nearly empty -> too risky
				yield true;
			}
			yield false;
		}
		};
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
		if (p.predictionsAreEmpty() && Arrays.stream(p.states()).anyMatch(m -> m == StateMachine.BALANCING)) {
			System.out.println("Fallback to DEFAULT BALANCING Schedule");
			return IntStream.range(0, p.numberOfPeriods()) //
					.mapToObj(period -> StateMachine.BALANCING) //
					.toArray(StateMachine[]::new);
		}

		var gtf = getInitialPopulation(p);
		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			var modes = IntStream.range(0, p.numberOfPeriods()) //
					.mapToObj(i -> p.states()[gt.get(i).get(0).intValue()]) //
					.toArray(StateMachine[]::new);
			return calculateCost(p, modes);
		};
		var engine = Engine //
				.builder(eval, gtf) //
				.executor(Runnable::run) // current thread
				.minimizing();
		if (populationSize != null) {
			engine.populationSize(1); //
		}
		Stream<EvolutionResult<IntegerGene, Double>> stream = engine.build().stream() //
				.limit(bySteadyFitness(20_000)) //
				.limit(byExecutionTime(ofMinutes(8))); //
		if (limit != null) {
			stream = stream.limit(limit); // apply optional limit
		}
		var bestGt = stream //
				.collect(toBestGenotype());
		return IntStream.range(0, p.numberOfPeriods()) //
				.mapToObj(period -> p.states()[bestGt.get(period).get(0).intValue()]) //
				.toArray(StateMachine[]::new);
	}

	protected static Genotype<IntegerGene> getInitialPopulation(Params p) {
		var result = new ArrayList<Chromosome<IntegerGene>>();

		// Add homogenous Chromosomes of one type
		IntStream.range(0, p.states().length) //
				.forEach(state -> {
					IntStream.range(0, p.numberOfPeriods()) //
							.mapToObj(i -> IntegerChromosome.of(IntegerGene.of(state, 0, p.states().length))) //
							.forEach(ch -> result.add(ch));
				});

		// TODO add serial optimized chromosome

		return Genotype.of(result);
	}
}
