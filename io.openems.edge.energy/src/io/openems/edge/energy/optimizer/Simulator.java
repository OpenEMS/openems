package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.buildInitialPopulation;
import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;
import io.openems.edge.energy.optimizer.SimulationResult.Period;

public class Simulator {

	/** Used to incorporate charge/discharge efficiency. */
	public static final double EFFICIENCY_FACTOR = 1.17;

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * @param gsc the {@link GlobalSimulationsContext}
	 * @param gt  the Schedule as a {@link Genotype}
	 * @return the cost, lower is better, always positive; {@link Double#NaN} on
	 *         error
	 */
	protected static double calculateCost(GlobalSimulationsContext gsc, Genotype<IntegerGene> gt) {
		return simulate(gsc, gt, null);
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * @param gsc     the {@link GlobalSimulationsContext}
	 * @param gt      the simulated {@link Genotype}
	 * @param collect a {@link Consumer} collecting context of the final result
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public static double simulate(GlobalSimulationsContext gsc, Genotype<IntegerGene> gt,
			Consumer<SimulationResult.Period> collect) {
		final var osc = OneSimulationContext.from(gsc);
		final var noOfPeriods = gsc.periods().size();

		var sum = 0.;
		for (var period = 0; period < noOfPeriods; period++) {
			sum += simulatePeriod(osc, gt, period, collect);
		}
		return sum;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param simulation  the {@link OneSimulationContext}
	 * @param gt          the simulated {@link Genotype}
	 * @param periodIndex the index of the simulated period
	 * @param collect     a {@link Consumer} collecting context of the final result
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public static double simulatePeriod(OneSimulationContext simulation, Genotype<IntegerGene> gt, int periodIndex,
			Consumer<SimulationResult.Period> collect) {
		final var period = simulation.global.periods().get(periodIndex);
		final var handlers = simulation.global.handlers();
		final var model = EnergyFlow.Model.from(simulation, period);

		var eshIndex = 0;
		for (var esh : handlers) {
			if (esh instanceof EnergyScheduleHandler.WithDifferentStates<?, ?> e) {
				// Simulate with state given by Genotype
				e.simulatePeriod(simulation, period, model, gt.get(eshIndex++).get(periodIndex).intValue());
			} else if (esh instanceof EnergyScheduleHandler.WithOnlyOneState<?> e) {
				e.simulatePeriod(simulation, period, model);
			}
		}

		final EnergyFlow energyFlow = model.solve();

		if (energyFlow == null) {
			LOG.error("Error while simulating period [" + periodIndex + "]");
			// TODO add configurable debug logging
			// LOG.info(simulation.toString());
			// model.logConstraints();
			// model.logMinMaxValues();
			return Double.POSITIVE_INFINITY;
		}

		// Calculate Cost
		// TODO should be done also by ESH to enable this use-case:
		// https://community.openems.io/t/limitierung-bei-negativen-preisen-und-lastgang-einkauf/2713/2
		double cost;
		if (energyFlow.getGrid() > 0) {
			// Filter negative prices
			var price = Math.max(0, period.price());

			cost = // Cost for direct Consumption
					energyFlow.getGridToCons() * price
							// Cost for future Consumption after storage
							+ energyFlow.getGridToEss() * price * EFFICIENCY_FACTOR;

		} else {
			// Sell-to-Grid
			cost = 0.;
		}
		if (collect != null) {
			// var postprocessedState = postprocessSimulatorState(state, ef);
			collect.accept(Period.from(period, energyFlow, simulation.getEssInitial()));
		}

		// Prepare for next period
		simulation.calculateEssInitial(energyFlow.getEss());

		return cost;
	}

	/**
	 * Runs the optimization and returns the "best" simulation result.
	 * 
	 * @param gsc                        the {@link GlobalSimulationsContext}
	 * @param previousResult             the {@link SimulationResult} of the
	 *                                   previous optimization run
	 * @param engineInterceptor          an interceptor for the
	 *                                   {@link Engine.Builder}
	 * @param evolutionStreamInterceptor an interceptor for the
	 *                                   {@link EvolutionStream}
	 * @return the best Schedule
	 */
	public static SimulationResult getBestSchedule(GlobalSimulationsContext gsc, SimulationResult previousResult,
			Function<Engine.Builder<IntegerGene, Double>, Engine.Builder<IntegerGene, Double>> engineInterceptor,
			Function<EvolutionStream<IntegerGene, Double>, EvolutionStream<IntegerGene, Double>> evolutionStreamInterceptor) {
		// Genotype:
		// - Separate IntegerChromosome per EnergyScheduleHandler WithDifferentStates
		// - Chromosome length = number of periods
		// - Integer-Genes represent the state
		final var chromosomes = gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.map(esh -> IntegerChromosome.of(0, esh.getAvailableStates().length, gsc.periods().size())) //
				.toList();
		if (chromosomes.isEmpty()) {
			return SimulationResult.EMPTY;
		}
		final var gtf = Genotype.of(chromosomes);

		var eval = (Function<Genotype<IntegerGene>, Double>) (gt) -> {
			gsc.simulationCounter().incrementAndGet();
			return calculateCost(gsc, gt);
		};

		final Executor executor;
		final var availableCores = Runtime.getRuntime().availableProcessors() - 1;
		if (availableCores > 1) {
			// Executor is a Thread-Pool with CPU-Cores minus one
			executor = new ForkJoinPool(availableCores);
			System.out.println("OPTIMIZER Executor runs on " + availableCores + " cores");
		} else {
			// Executor is the current thread
			executor = Runnable::run;
			System.out.println("OPTIMIZER Executor runs on current thread");
		}

		var engine = Engine //
				.builder(eval, gtf) //
				.executor(executor) //
				.minimizing();
		if (engineInterceptor != null) {
			engine = engineInterceptor.apply(engine);
		}
		EvolutionStream<IntegerGene, Double> stream = engine.build() //
				.stream(buildInitialPopulation(gsc, previousResult)) //
				.limit(result -> !currentThread().isInterrupted());
		if (evolutionStreamInterceptor != null) {
			stream = evolutionStreamInterceptor.apply(stream);
		}
		var bestGt = stream //
				.collect(toBestGenotype());

		return SimulationResult.fromQuarters(gsc, bestGt);
	}
}
