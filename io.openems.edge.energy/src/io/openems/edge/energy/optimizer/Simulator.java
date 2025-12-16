package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.EvolutionResult.toBestResult;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static java.lang.Math.max;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.jenetics.EliteSelector;
import io.jenetics.GaussianMutator;
import io.jenetics.Gene;
import io.jenetics.IntegerGene;
import io.jenetics.ShiftMutator;
import io.jenetics.ShuffleMutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.TournamentSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.optimizer.ModeCombinations.Mode;
import io.openems.edge.energy.optimizer.ModeCombinations.ModeCombination;

public class Simulator {

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);

	public final GlobalOptimizationContext goc;
	public final ModeCombinations modeCombinations;

	private final AtomicInteger simulationsCounter = new AtomicInteger(0);

	public Simulator(GlobalOptimizationContext goc) {
		this.goc = goc;

		// Initialize the EnergyScheduleHandlers.
		for (var esh : goc.eshs()) {
			((AbstractEnergyScheduleHandler<?, ?>) esh /* this is safe */).initialize(goc);
		}
		this.modeCombinations = ModeCombinations.fromGlobalOptimizationContext(goc);
	}

	protected int getTotalNumberOfSimulations() {
		return this.simulationsCounter.get();
	}

	protected static Fitness simulate(GlobalOptimizationContext goc, ModeCombinations modeCombinations, int[] schedule,
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
		final var fitness = new Fitness();

		for (var periodIndex = 0; periodIndex < noOfPeriods; periodIndex++) {
			var modeCombination = modeCombinations.get(schedule[periodIndex]);
			simulatePeriod(gsc, cscs, periodIndex, modeCombination, fitness, bestScheduleCollector);
		}

		return fitness;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param gsc                   the {@link GlobalScheduleContext}
	 * @param cscs                  the ControllerScheduleContexts
	 * @param periodIndex           the index of the simulated period
	 * @param modeCombination       the {@link ModeCombination} of the simulated
	 *                              period
	 * @param bestScheduleCollector the {@link BestScheduleCollector}; or null
	 * @param fitness               the {@link Fitness} result
	 */
	public static void simulatePeriod(//
			GlobalScheduleContext gsc, //
			ImmutableMap<EnergyScheduleHandler, Object> cscs, //
			int periodIndex, //
			ModeCombination modeCombination, //
			Fitness fitness, //
			BestScheduleCollector bestScheduleCollector) {
		final var period = gsc.goc.periods().get(periodIndex);
		final var eshs = gsc.goc.eshs();

		final EnergyFlow.Model ef;
		try {
			ef = EnergyFlow.Model.from(gsc, period);
		} catch (OpenemsException e) {
			LOG.error("Error while simulating period [" + periodIndex + "]", e);
			fitness.addHardConstraintViolation();
			return;
		}

		final var eshModes = ImmutableMap.<EnergyScheduleHandler.WithDifferentModes, Integer>builder();

		var eshsWithDifferentModesIndex = 0;
		for (var esh : eshs) {
			try {
				var csc = cscs.get(esh);
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentModes e -> {
					final var modeIndex = e.modes().isEmpty() //
							? -1 // none available
							: Optional.ofNullable(modeCombination.mode(eshsWithDifferentModesIndex++)) //
									.map(Mode::index) //
									.orElse(-1); // none available
					final var preProcessedMode = e.preProcessPeriod(period, gsc, modeIndex);
					eshModes.put(e, preProcessedMode);
					e.simulate(period, gsc, csc, ef, preProcessedMode, fitness);
				}
				case EnergyScheduleHandler.WithOnlyOneMode e //
					-> e.simulate(period, gsc, csc, ef, fitness);
				}

			} catch (RuntimeException e) {
				throw new RuntimeException("Error during simulation of [" + esh.getParentId() + "] " //
						+ "Period [" + period.index() + "/" + period.time() + "]: " + e.getMessage(), e);
			}
		}

		final EnergyFlow energyFlow = ef.solve();

		// Calculate Grid-Buy Cost
		if (energyFlow.getGrid() > 0) {
			// Filter negative prices
			var price = max(0, period.price());

			int buyFromGrid = max(0, energyFlow.getGrid());
			int chargeEss = max(0, -energyFlow.getEss());
			int gridToEss = Math.min(buyFromGrid, chargeEss);
			int gridToCons = buyFromGrid - gridToEss;
			fitness.addGridBuyCost(
					// Cost for direct Consumption
					gridToCons * price
							// Cost for future Consumption after storage
							+ max(0, gridToEss) * price * gsc.goc.riskLevel().efficiencyFactor);
		}

		// Calculate Grid-Sell Revenue
		if (energyFlow.getGrid() < 0) {
			// Filter negative prices
			var price = max(0, period.price());

			int sellToGrid = max(0, -energyFlow.getGrid());
			int dischargeEnergy = max(0, energyFlow.getEss());
			int essToGrid = Math.min(sellToGrid, dischargeEnergy);
			fitness.addGridSellRevenue(//
					// Revenue for Discharge-to-Grid
					essToGrid * price);
		}

		if (bestScheduleCollector != null) {
			final var srp = SimulationResult.Period.from(period, modeCombination, energyFlow,
					gsc.ess.getInitialEnergy());
			bestScheduleCollector.allPeriods.accept(srp);
			final var eshModesMap = eshModes.build();
			for (var esh : eshs) {
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentModes e //
					-> bestScheduleCollector.eshModes.accept(new EshToMode(e, srp, //
							e.postProcessPeriod(period, gsc, energyFlow, eshModesMap.get(e))));
				case EnergyScheduleHandler.WithOnlyOneMode e //
					-> doNothing();
				}
			}
		}

		// Prepare for next period
		gsc.ess.calculateInitialEnergy(energyFlow.getEss());
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
	public SimulationResult getBestSchedule(//
			SimulationResult previousResult, //
			boolean isCurrentPeriodFixed,
			Function<Engine.Builder<IntegerGene, Fitness>, Engine.Builder<IntegerGene, Fitness>> engineInterceptor, //
			Function<EvolutionStream<IntegerGene, Fitness>, EvolutionStream<IntegerGene, Fitness>> evolutionStreamInterceptor) {
		final var codec = EshCodec.of(this.goc, this.modeCombinations, previousResult, isCurrentPeriodFixed);
		if (codec == null) {
			// TODO if there are ESHs we should return the fixed Schedule as
			// SimulationResult
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
				.builder(gt -> {
					this.simulationsCounter.incrementAndGet();
					return simulate(this.goc, this.modeCombinations, gt, null);
				}, codec) //
				.selector(//
						new EliteSelector<IntegerGene, Fitness>(populationSize / 4, //
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
				.limit(result -> !Thread.currentThread().isInterrupted());
		if (evolutionStreamInterceptor != null) {
			stream = evolutionStreamInterceptor.apply(stream);
		}

		// Start the evaluation
		var bestGt = stream //
				.collect(toBestResult(codec));
		if (bestGt == null) {
			return EMPTY_SIMULATION_RESULT;
		}
		return SimulationResult.fromQuarters(this.goc, bestGt, this.simulationsCounter.get());
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
	 * Serialize.
	 * 
	 * @return the {@link JsonObject}
	 */
	public JsonObject toJson() {
		return buildJsonObject() //
				.add("GlobalOptimizationContext", GlobalOptimizationContext.toJson(this.goc)) //
				.build();
	}
}
