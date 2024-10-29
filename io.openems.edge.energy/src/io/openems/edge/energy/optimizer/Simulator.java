package io.openems.edge.energy.optimizer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.jenetics.engine.EvolutionResult.toBestGenotype;
import static io.openems.edge.energy.optimizer.QuickSchedules.fromExistingSimulationResult;
import static java.lang.Thread.currentThread;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;

public class Simulator {

	/** Used to incorporate charge/discharge efficiency. */
	public static final double EFFICIENCY_FACTOR = 1.17;

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

	public final GlobalSimulationsContext gsc;

	protected final LoadingCache<Genotype<IntegerGene>, Double> cache;

	public Simulator(GlobalSimulationsContext gsc) {
		this.gsc = gsc;
		this.cache = CacheBuilder.newBuilder() //
				.recordStats() //
				.build(new CacheLoader<Genotype<IntegerGene>, Double>() {

					@Override
					/**
					 * Simulates a Schedule and calculates the cost.
					 * 
					 * <p>
					 * NOTE: do not throw an Exception here, because we use
					 * {@link LoadingCache#getUnchecked(Object)} below.
					 * 
					 * @param gt the Schedule as a {@link Genotype}
					 * @return the cost, lower is better, always positive; {@link Double#NaN} on
					 *         error
					 */
					public Double load(final Genotype<IntegerGene> gt) {
						return simulate(Simulator.this.gsc, gt, null);
					}
				});

		// Initialize the EnergyScheduleHandlers.
		for (var esh : gsc.handlers()) {
			((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
		}
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * <p>
	 * This method internally uses a Cache for {@link Genotype}s.
	 * 
	 * @param gt the Schedule as a {@link Genotype}
	 * @return the cost, lower is better, always positive; {@link Double#NaN} on
	 *         error
	 */
	public double calculateCost(Genotype<IntegerGene> gt) {
		return this.cache.getUnchecked(gt);
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * <p>
	 * This method does not a Cache for {@link Genotype}s.
	 * 
	 * @param gt                    the simulated {@link Genotype}
	 * @param bestScheduleCollector the {@link BestScheduleCollector}
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public double simulate(Genotype<IntegerGene> gt, BestScheduleCollector bestScheduleCollector) {
		return simulate(this.gsc, gt, bestScheduleCollector);
	}

	protected static double simulate(GlobalSimulationsContext gsc, Genotype<IntegerGene> gt,
			BestScheduleCollector bestScheduleCollector) {
		final var osc = OneSimulationContext.from(gsc);
		final var noOfPeriods = gsc.periods().size();

		var sum = 0.;
		for (var period = 0; period < noOfPeriods; period++) {
			sum += simulatePeriod(osc, gt, period, bestScheduleCollector);
		}
		return sum;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param simulation            the {@link OneSimulationContext}
	 * @param gt                    the simulated {@link Genotype}
	 * @param periodIndex           the index of the simulated period
	 * @param bestScheduleCollector the {@link BestScheduleCollector}; or null
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public static double simulatePeriod(OneSimulationContext simulation, Genotype<IntegerGene> gt, int periodIndex,
			BestScheduleCollector bestScheduleCollector) {
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
		if (bestScheduleCollector != null) {
			final var srp = SimulationResult.Period.from(period, energyFlow, simulation.getEssInitial());
			bestScheduleCollector.allPeriods.accept(srp);
			eshIndex = 0;
			for (var esh : handlers) {
				if (esh instanceof EnergyScheduleHandler.WithDifferentStates<?, ?> e) {
					bestScheduleCollector.eshStates.accept(new EshToState(e, srp, //
							e.postProcessPeriod(period, simulation, energyFlow,
									gt.get(eshIndex++).get(periodIndex).intValue())));
				}
			}
		}

		// Prepare for next period
		simulation.calculateEssInitial(energyFlow.getEss());

		return cost;
	}

	/**
	 * Runs the optimization and returns the "best" simulation result.
	 * 
	 * @param previousResult             the {@link SimulationResult} of the
	 *                                   previous optimization run
	 * @param engineInterceptor          an interceptor for the
	 *                                   {@link Engine.Builder}
	 * @param evolutionStreamInterceptor an interceptor for the
	 *                                   {@link EvolutionStream}
	 * @return the best Schedule
	 */
	public SimulationResult getBestSchedule(SimulationResult previousResult,
			Function<Engine.Builder<IntegerGene, Double>, Engine.Builder<IntegerGene, Double>> engineInterceptor,
			Function<EvolutionStream<IntegerGene, Double>, EvolutionStream<IntegerGene, Double>> evolutionStreamInterceptor) {
		// Genotype:
		// - Separate IntegerChromosome per EnergyScheduleHandler WithDifferentStates
		// - Chromosome length = number of periods
		// - Integer-Genes represent the state
		final var chromosomes = this.gsc.handlers().stream() //
				.filter(EnergyScheduleHandler.WithDifferentStates.class::isInstance) //
				.map(EnergyScheduleHandler.WithDifferentStates.class::cast) //
				.map(esh -> IntegerChromosome.of(0, esh.getAvailableStates().length, this.gsc.periods().size())) //
				.toList();
		if (chromosomes.isEmpty()) {
			return SimulationResult.EMPTY;
		}
		final var gtf = Genotype.of(chromosomes);

		// Decide for single- or multi-threading
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

		// Build the Jenetics Engine
		var engine = Engine //
				.builder(this.cache::getUnchecked, gtf) //
				.executor(executor) //
				.minimizing();
		if (engineInterceptor != null) {
			engine = engineInterceptor.apply(engine);
		}

		// Start with previous simulation result as initial population if available
		var initialPopulation = fromExistingSimulationResult(this.gsc, previousResult);
		EvolutionStream<IntegerGene, Double> stream;
		if (previousResult != null) {
			stream = engine.build().stream(List.of(initialPopulation));
		} else {
			stream = engine.build().stream();
		}
		stream = stream.limit(result -> !currentThread().isInterrupted());
		if (evolutionStreamInterceptor != null) {
			stream = evolutionStreamInterceptor.apply(stream);
		}

		// Start the evaluation
		var bestGt = stream //
				.collect(toBestGenotype());

		return SimulationResult.fromQuarters(this.gsc, bestGt);
	}

	protected static record BestScheduleCollector(//
			Consumer<SimulationResult.Period> allPeriods, //
			Consumer<EshToState> eshStates) {
	}

	protected static record EshToState(//
			EnergyScheduleHandler.WithDifferentStates<?, ?> esh, //
			SimulationResult.Period period, //
			int postProcessedStateIndex) {
	}

	/**
	 * Builds a log string of this {@link Simulator}.
	 * 
	 * @param prefix a line prefix
	 * @return log string
	 */
	public String toLogString(String prefix) {
		return prefix + toStringHelper(this) //
				.addValue(this.gsc) //
				.addValue(this.cache.stats()) //
				.toString();
	}
}
