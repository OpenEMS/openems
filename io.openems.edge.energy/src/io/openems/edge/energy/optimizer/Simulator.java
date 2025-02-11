package io.openems.edge.energy.optimizer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.jenetics.engine.EvolutionResult.toBestResult;
import static io.openems.edge.energy.optimizer.InitialPopulation.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static java.lang.Thread.currentThread;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.Mutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.EnergyScheduleHandler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;
import io.openems.edge.energy.api.simulation.OneSimulationContext;

public class Simulator {

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

	public final GlobalSimulationsContext gsc;

	protected final LoadingCache<int[][], Double> cache;

	public Simulator(GlobalSimulationsContext gsc) {
		this.gsc = gsc;
		this.cache = CacheBuilder.newBuilder() //
				.recordStats() //
				.build(new CacheLoader<int[][], Double>() {

					@Override
					/**
					 * Simulates a Schedule and calculates the cost.
					 * 
					 * <p>
					 * NOTE: do not throw an Exception here, because we use
					 * {@link LoadingCache#getUnchecked(Object)} below.
					 * 
					 * @param schedule the schedule as defined by {@link EshCodec}
					 * @return the cost, lower is better, always positive; {@link Double#NaN} on
					 *         error
					 */
					public Double load(final int[][] schedule) {
						return simulate(Simulator.this.gsc, schedule, null);
					}
				});

		// Initialize the EnergyScheduleHandlers.
		for (var esh : gsc.eshs()) {
			((AbstractEnergyScheduleHandler<?>) esh /* this is safe */).initialize(gsc);
		}
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * <p>
	 * This method internally uses a Cache for schedule costs.
	 * 
	 * @param schedule the schedule as defined by {@link EshCodec}
	 * @return the cost, lower is better, always positive; {@link Double#NaN} on
	 *         error
	 */
	public double calculateCost(int[][] schedule) {
		return this.cache.getUnchecked(schedule);
	}

	/**
	 * Simulates a Schedule and calculates the cost.
	 * 
	 * <p>
	 * This method does not a Cache for {@link Genotype}s.
	 * 
	 * @param schedule              the schedule as defined by {@link EshCodec}
	 * @param bestScheduleCollector the {@link BestScheduleCollector}
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public double simulate(int[][] schedule, BestScheduleCollector bestScheduleCollector) {
		return simulate(this.gsc, schedule, bestScheduleCollector);
	}

	protected static double simulate(GlobalSimulationsContext gsc, int[][] schedule,
			BestScheduleCollector bestScheduleCollector) {
		final var osc = OneSimulationContext.from(gsc);
		final var noOfPeriods = gsc.periods().size();

		var sum = 0.;
		for (var period = 0; period < noOfPeriods; period++) {
			sum += simulatePeriod(osc, schedule, period, bestScheduleCollector);
		}
		return sum;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param simulation            the {@link OneSimulationContext}
	 * @param schedule              the schedule as defined by {@link EshCodec}
	 * @param periodIndex           the index of the simulated period
	 * @param bestScheduleCollector the {@link BestScheduleCollector}; or null
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public static double simulatePeriod(OneSimulationContext simulation, int[][] schedule, int periodIndex,
			BestScheduleCollector bestScheduleCollector) {
		final var period = simulation.global.periods().get(periodIndex);
		final var eshs = simulation.global.eshs();
		final var model = EnergyFlow.Model.from(simulation, period);

		double cost = 0.;
		var eshIndex = 0;
		for (var esh : eshs) {
			switch (esh) {
			case EnergyScheduleHandler.WithDifferentStates<?, ?> e //
				-> cost += e.simulatePeriod(simulation, period, model, schedule[periodIndex][eshIndex++]);
			case EnergyScheduleHandler.WithOnlyOneState<?> e //
				-> e.simulatePeriod(simulation, period, model);
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
		if (energyFlow.getGrid() > 0) {
			// Filter negative prices
			var price = Math.max(0, period.price());

			cost += // Cost for direct Consumption
					energyFlow.getGridToCons() * price
							// Cost for future Consumption after storage
							+ energyFlow.getGridToEss() * price * simulation.global.riskLevel().efficiencyFactor;

		} else {
			// Sell-to-Grid -> no cost
		}
		if (bestScheduleCollector != null) {
			final var srp = SimulationResult.Period.from(period, energyFlow, simulation.ess.getInitialEnergy());
			bestScheduleCollector.allPeriods.accept(srp);
			eshIndex = 0;
			for (var esh : eshs) {
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentStates<?, ?> e //
					-> bestScheduleCollector.eshStates.accept(new EshToState(e, srp, //
							e.postProcessPeriod(period, simulation, energyFlow, schedule[periodIndex][eshIndex++])));
				case EnergyScheduleHandler.WithOnlyOneState<?> e //
					-> FunctionUtils.doNothing();
				}
			}
		}

		// Prepare for next period
		simulation.ess.calculateInitialEnergy(energyFlow.getEss());

		return cost;
	}

	/**
	 * Runs the optimization and returns the "best" simulation result.
	 * 
	 * @param previousResult             the {@link SimulationResult} of the
	 *                                   previous optimization run
	 * @param isCurrentPeriodFixed       fixes the {@link Gene} of the current
	 *                                   period to the previousResult
	 * @param engineInterceptor          an interceptor for the
	 *                                   {@link Engine.Builder}
	 * @param evolutionStreamInterceptor an interceptor for the
	 *                                   {@link EvolutionStream}
	 * @return the best Schedule
	 */
	public SimulationResult getBestSchedule(SimulationResult previousResult, boolean isCurrentPeriodFixed,
			Function<Engine.Builder<IntegerGene, Double>, Engine.Builder<IntegerGene, Double>> engineInterceptor,
			Function<EvolutionStream<IntegerGene, Double>, EvolutionStream<IntegerGene, Double>> evolutionStreamInterceptor) {
		final var codec = EshCodec.of(this.gsc, previousResult, isCurrentPeriodFixed);
		if (codec == null) {
			return EMPTY_SIMULATION_RESULT;
		}

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
		final var initialPopulation = generateInitialPopulation(this.gsc, codec, previousResult, isCurrentPeriodFixed);
		var engine = Engine //
				.builder(this.cache::getUnchecked, codec) //
				.alterers(//
						new SinglePointCrossover<IntegerGene, Double>(0.2), //
						new Mutator<IntegerGene, Double>(0.15)) //
				.populationSize(initialPopulation.size()) //
				.executor(executor) //
				.minimizing();
		if (engineInterceptor != null) {
			engine = engineInterceptor.apply(engine);
		}

		var stream = engine.build() //
				.stream(initialPopulation) //
				.limit(result -> !currentThread().isInterrupted());
		if (evolutionStreamInterceptor != null) {
			stream = evolutionStreamInterceptor.apply(stream);
		}

		// Start the evaluation
		var bestGt = stream //
				.collect(toBestResult(codec));
		if (bestGt == null) {
			return EMPTY_SIMULATION_RESULT;
		}
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
