package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.paramsAreValid;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.postprocessSimulatorState;
import static java.lang.Math.max;
import static java.time.Duration.ofSeconds;

import java.time.ZonedDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSortedMap;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Params.OptimizePeriod;

public class Simulator {

	/** Used to incorporate charge/discharge efficiency. */
	public static final double EFFICIENCY_FACTOR = 1.17;

	public record Period(OptimizePeriod op, StateMachine state, int essInitial, EnergyFlow ef) {
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * @param p        the {@link Params}
	 * @param schedule the {@link StateMachine} states of the Schedule
	 * @return the cost, lower is better; always positive
	 */
	protected static double calculateCost(Params p, StateMachine[] schedule) {
		final var nextEssInitial = new AtomicInteger(p.essInitialEnergy());
		var sum = 0.;
		for (var i = 0; i < p.optimizePeriods().size(); i++) {
			sum += simulatePeriod(p, p.optimizePeriods().get(i), schedule[i], nextEssInitial, null);
		}
		return sum;
	}

	/**
	 * Simulates a Schedule in quarterly periods.
	 * 
	 * @param p        the {@link Params}
	 * @param schedule the {@link StateMachine} states of the Schedule
	 * @return a Map of {@link Period}s
	 */
	protected static ImmutableSortedMap<ZonedDateTime, Period> simulate(Params p, StateMachine[] schedule) {
		final var nextEssInitial = new AtomicInteger(p.essInitialEnergy());
		var result = ImmutableSortedMap.<ZonedDateTime, Period>naturalOrder();
		for (var i = 0; i < p.optimizePeriods().size(); i++) {
			var state = schedule[i];
			var op = p.optimizePeriods().get(i);
			// Convert mixed OptimizePeriods to pure quarterly
			for (var qp : op.quarterPeriods()) {
				var quarterlyOp = new OptimizePeriod(qp.time(), qp.essMaxEnergy(), qp.essChargeInChargeGrid(),
						qp.maxBuyFromGrid(), qp.production(), qp.consumption(), qp.price(), ImmutableList.of(qp));
				simulatePeriod(p, quarterlyOp, state, nextEssInitial, period -> result.put(period.op().time(), period));
			}
		}
		return result.build();
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param p              the {@link Params}
	 * @param op             the current {@link OptimizePeriod}
	 * @param state          the {@link StateMachine} of the current period
	 * @param nextEssInitial the initial SoC-Energy; also used as return value
	 * @param collect        a {@link Consumer} to collect the simulation results if
	 *                       required. We are not always collecting results to
	 *                       reduce workload during simulation.
	 * @return the cost, lower is better; always positive
	 */
	protected static double simulatePeriod(Params p, OptimizePeriod op, StateMachine state,
			final AtomicInteger nextEssInitial, Consumer<Period> collect) {
		// Constants
		final var essInitial = max(0, nextEssInitial.get()); // always at least '0'

		// Calculate Energy-Flow
		final var ef = switch (state) {
		case BALANCING -> EnergyFlow.withBalancing(p, op, essInitial);
		case DELAY_DISCHARGE -> EnergyFlow.withDelayDischarge(p, op, essInitial);
		case CHARGE_GRID -> EnergyFlow.withChargeGrid(p, op, essInitial);
		};

		nextEssInitial.set(essInitial - ef.ess());

		// Calculate Cost
		double cost;
		if (ef.grid() > 0) {
			cost = // Cost for direct Consumption
					ef.gridToConsumption() * op.price()
							// Cost for future Consumption after storage
							+ ef.gridToEss() * op.price() * EFFICIENCY_FACTOR;

		} else {
			// Sell-to-Grid
			cost = 0.;
		}
		if (collect != null) {
			var postprocessedState = postprocessSimulatorState(p, essInitial, state, ef);
			collect.accept(new Period(op, postprocessedState, essInitial, ef));
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
			return p.optimizePeriods().stream() //
					.map(op -> StateMachine.BALANCING) //
					.toArray(StateMachine[]::new);
		}

		var gtf = Genotype.of(IntegerChromosome.of(IntegerGene.of(0, p.states().length)), p.optimizePeriods().size()); //
		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			var modes = new StateMachine[p.optimizePeriods().size()];
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
		return IntStream.range(0, p.optimizePeriods().size()) //
				.mapToObj(period -> p.states()[bestGt.get(period).get(0).intValue()]) //
				.toArray(StateMachine[]::new);
	}
}
