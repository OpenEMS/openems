package io.openems.edge.energy.optimizer;

import static com.google.common.base.MoreObjects.toStringHelper;
import static io.jenetics.engine.EvolutionResult.toBestResult;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static java.lang.Math.max;
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
import com.google.common.collect.ImmutableMap;

import io.jenetics.EliteSelector;
import io.jenetics.GaussianMutator;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.IntegerGene;
import io.jenetics.ShiftMutator;
import io.jenetics.ShuffleMutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.TournamentSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;

public class Simulator {

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

	public final GlobalOptimizationContext goc;

	protected final LoadingCache<int[][], Double> cache;

	public Simulator(GlobalOptimizationContext goc) {
		this.goc = goc;
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
						return simulate(Simulator.this.goc, schedule, null);
					}
				});

		// Initialize the EnergyScheduleHandlers.
		for (var esh : goc.eshs()) {
			((AbstractEnergyScheduleHandler<?, ?>) esh /* this is safe */).initialize(goc);
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
		return simulate(this.goc, schedule, bestScheduleCollector);
	}

	protected static double simulate(GlobalOptimizationContext goc, int[][] schedule,
			BestScheduleCollector bestScheduleCollector) {
		final var gsc = GlobalScheduleContext.from(goc);
		final var cscsBuilder = ImmutableMap.<EnergyScheduleHandler, Object>builder();
		for (var esh : goc.eshs()) {
			var csc = esh.createScheduleContext();
			if (csc != null) {
				cscsBuilder.put(esh, csc);
			}
		}
		final var cscs = cscsBuilder.build();
		final var noOfPeriods = goc.periods().size();

		var sum = 0.;
		for (var period = 0; period < noOfPeriods; period++) {
			sum += simulatePeriod(gsc, cscs, schedule, period, bestScheduleCollector);
		}
		return sum;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param gsc                   the {@link GlobalScheduleContext}
	 * @param cscs                  the ControllerScheduleContexts
	 * @param schedule              the schedule as defined by {@link EshCodec}
	 * @param periodIndex           the index of the simulated period
	 * @param bestScheduleCollector the {@link BestScheduleCollector}; or null
	 * @return the cost, lower is better, always positive;
	 *         {@link Double#POSITIVE_INFINITY} on error
	 */
	public static double simulatePeriod(GlobalScheduleContext gsc, ImmutableMap<EnergyScheduleHandler, Object> cscs,
			int[][] schedule, int periodIndex, BestScheduleCollector bestScheduleCollector) {
		final var period = gsc.goc.periods().get(periodIndex);
		final var eshs = gsc.goc.eshs();
		final var ef = EnergyFlow.Model.from(gsc, period);

		double cost = 0.;
		var eshIndex = 0;
		for (var esh : eshs) {
			var csc = cscs.get(esh);
			switch (esh) {
			case EnergyScheduleHandler.WithDifferentModes e //
				-> cost += e.simulate(period, gsc, csc, ef, schedule[periodIndex][eshIndex++]);
			case EnergyScheduleHandler.WithOnlyOneMode e //
				-> e.simulate(period, gsc, csc, ef);
			}
		}

		final EnergyFlow energyFlow = ef.solve();

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
			var price = max(0, period.price());

			cost += // Cost for direct Consumption
					energyFlow.getGridToCons() * price
							// Cost for future Consumption after storage
							+ energyFlow.getGridToEss() * price * gsc.goc.riskLevel().efficiencyFactor;

		} else {
			// Sell-to-Grid -> no cost
		}
		if (bestScheduleCollector != null) {
			final var srp = SimulationResult.Period.from(period, energyFlow, gsc.ess.getInitialEnergy());
			bestScheduleCollector.allPeriods.accept(srp);
			eshIndex = 0;
			for (var esh : eshs) {
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentModes e //
					-> bestScheduleCollector.eshModes.accept(new EshToMode(e, srp, //
							e.postProcessPeriod(period, gsc, energyFlow, schedule[periodIndex][eshIndex++])));
				case EnergyScheduleHandler.WithOnlyOneMode e //
					-> FunctionUtils.doNothing();
				}
			}
		}

		// Prepare for next period
		gsc.ess.calculateInitialEnergy(energyFlow.getEss());

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
		final var codec = EshCodec.of(this.goc, previousResult, isCurrentPeriodFixed);
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
		final var initialPopulation = generateInitialPopulation(codec);
		var populationSize = fitWithin(10, 50, initialPopulation.population().size() * 2);

		var engine = Engine //
				.builder(this.cache::getUnchecked, codec) //
				.selector(//
						new EliteSelector<IntegerGene, Double>(populationSize / 4, //
								new TournamentSelector<>(3)))
				.alterers(//
						new ShiftMutator<>(), //
						new ShuffleMutator<>(), //
						new SinglePointCrossover<>(), //
						new GaussianMutator<>()) //
				.populationSize(populationSize) //
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
		return SimulationResult.fromQuarters(this.goc, bestGt);
	}

	protected static record BestScheduleCollector(//
			Consumer<SimulationResult.Period> allPeriods, //
			Consumer<EshToMode> eshModes) {
	}

	protected static record EshToMode(//
			EnergyScheduleHandler.WithDifferentModes esh, //
			SimulationResult.Period period, //
			int postProcessedModeIndex) {
	}

	/**
	 * Builds a log string of this {@link Simulator}.
	 * 
	 * @param prefix a line prefix
	 * @return log string
	 */
	public String toLogString(String prefix) {
		return prefix + toStringHelper(this) //
				.addValue(this.goc) //
				.addValue(this.cache.stats()) //
				.toString();
	}
}
