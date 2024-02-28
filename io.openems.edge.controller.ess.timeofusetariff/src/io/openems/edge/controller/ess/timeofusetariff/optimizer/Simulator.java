package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateBalancingEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateChargeGridEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateMaxChargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateMaxDischargeEnergy;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.paramsAreValid;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessSimulatorState;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.time.Duration.ofSeconds;

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
	public static final double EFFICIENCY_FACTOR = 1.17;

	/**
	 * Calculates the cost of a Schedule.
	 * 
	 * @param p      the {@link Params}
	 * @param states the {@link StateMachine} states of the Schedule
	 * @return the cost, lower is better; always positive
	 */
	protected static double calculateCost(Params p, StateMachine[] states) {
		return calculateCost(p, states, null);
	}

	/**
	 * Calculates the cost of a Schedule.
	 * 
	 * @param p       the {@link Params}
	 * @param states  the {@link StateMachine} states of the Schedule
	 * @param collect a {@link Consumer} to collect the simulation results if
	 *                required. We are not always collecting results to reduce
	 *                workload during simulation.
	 * @return the cost, lower is better; always positive
	 */
	protected static double calculateCost(Params p, StateMachine[] states, Consumer<Period> collect) {
		final var nextEssInitial = new AtomicInteger(p.essInitialEnergy());
		var sum = 0.;
		for (var i = 0; i < p.numberOfPeriods(); i++) {
			sum += calculatePeriodCost(p, i, states, nextEssInitial, collect);
		}
		return sum;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param p              the {@link Params}
	 * @param i              the index of the current period
	 * @param states         the {@link StateMachine} states of the Schedule
	 * @param nextEssInitial the initial SoC-Energy; also used as return value
	 * @param collect        a {@link Consumer} to collect the simulation results if
	 *                       required. We are not always collecting results to
	 *                       reduce workload during simulation.
	 * @return the cost, lower is better; always positive
	 */
	protected static double calculatePeriodCost(Params p, int i, StateMachine[] states,
			final AtomicInteger nextEssInitial, Consumer<Period> collect) {
		// Constants
		final var production = p.productions()[i];
		final var consumption = p.consumptions()[i];
		final var state = states[i];
		final var price = p.prices()[i];
		final var essInitial = max(0, nextEssInitial.get()); // always at least '0'

		// Calculate Grid and ESS energy
		final var essMaxChargeInBalancing = calculateMaxChargeEnergy(//
				p.essTotalEnergy() /* unlimited in BALANCING */, //
				p.essMaxEnergyPerPeriod(), essInitial);
		final var essMaxDischarge = calculateMaxDischargeEnergy(p.essMinSocEnergy(), //
				p.essMaxEnergyPerPeriod(), essInitial);
		final var essChargeDischargeInBalancing = calculateBalancingEnergy(essMaxChargeInBalancing, essMaxDischarge,
				production, consumption);
		final int essChargeDischarge;
		final int grid;
		final int gridEssCharge;
		final int gridConsumption;
		switch (state) {
		case BALANCING:
			essChargeDischarge = essChargeDischargeInBalancing;
			grid = consumption - production - essChargeDischarge;
			gridEssCharge = 0;
			gridConsumption = grid;
			break;

		case DELAY_DISCHARGE:
			essChargeDischarge = min(0, essChargeDischargeInBalancing); // allow charge
			grid = consumption - production - essChargeDischarge;
			gridEssCharge = 0;
			gridConsumption = grid - gridEssCharge;
			break;

		case CHARGE_GRID:
			final var essChargeInChargeGrid = calculateMaxChargeEnergy(//
					p.essMaxSocEnergy() /* limited in CHARGE_GRID */, //
					p.essMaxEnergyPerPeriod(), essInitial);

			gridConsumption = max(0, consumption - production);
			gridEssCharge = calculateChargeGridEnergy(essChargeInChargeGrid, p.essChargeInChargeGrid(),
					p.maxBuyFromGrid(), production, consumption);
			grid = gridConsumption + gridEssCharge;
			essChargeDischarge = consumption - production - grid;
			break;

		default:
			throw new IllegalArgumentException("Missing handler for State [" + state + "]. This should never happen!");
		}
		nextEssInitial.set(essInitial - essChargeDischarge);

		// Calculate Cost
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
			var postprocessedState = postprocessSimulatorState(p, essChargeDischarge, essInitial,
					essChargeDischargeInBalancing, state);
			collect.accept(new Period(time, production, consumption, essInitial, postprocessedState, essChargeDischarge,
					grid, price, cost / 1000000));
		}
		return cost;
	}

	/**
	 * Runs the optimization with default settings.
	 * 
	 * @param p                     the {@link Params}
	 * @param executionLimitSeconds limit.byExecutionTime.ofSeconds
	 * @return the best schedule
	 */
	protected static StateMachine[] getBestSchedule(Params p, long executionLimitSeconds) {
		return getBestSchedule(p, executionLimitSeconds, null, null);
	}

	protected static StateMachine[] getBestSchedule(Params p, long executionLimitSeconds, Integer populationSize,
			Integer limit) {
		// Return pure BALANCING Schedule if no predictions are available
		if (!paramsAreValid(p)) {
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
				.limit(byExecutionTime(ofSeconds(executionLimitSeconds))); //
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
