package io.openems.edge.energy.optimizer;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static java.lang.Math.max;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.jenetics.EliteSelector;
import io.jenetics.GaussianMutator;
import io.jenetics.Gene;
import io.jenetics.IntegerGene;
import io.jenetics.Phenotype;
import io.jenetics.ShiftMutator;
import io.jenetics.ShuffleMutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.TournamentSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext.Period;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.api.simulation.GocUtils;
import io.openems.edge.energy.optimizer.ModeCombinations.Mode;
import io.openems.edge.energy.optimizer.ModeCombinations.ModeCombination;
import io.openems.edge.energy.optimizer.SimulationResult.BestScheduleCollector;

public class Simulator {

	private static final Logger LOG = LoggerFactory.getLogger(Simulator.class);
	private static final double EFFICIENCY_FACTOR = 1.17;

	public final GlobalOptimizationContext goc;
	public final ModeCombinations modeCombinations;
	private final List<Map<Integer, Double>> normalizedEshModePreferenceRanks;

	private final AtomicInteger simulationsCounter = new AtomicInteger(0);
	private final AtomicLong generationsCounter = new AtomicLong(0);

	private Duration earliestCallbackDelay = Duration.ofSeconds(30);

	public Simulator(GlobalOptimizationContext goc) {
		this.goc = goc;

		// Initialize the EnergyScheduleHandlers
		for (var esh : goc.eshs()) {
			var coc = ((AbstractEnergyScheduleHandler<?, ?>) esh /* this is safe */).initialize(goc);
			LOG.info("OPTIMIZER ControllerOptimizationContext '" + esh.getParentId() + "': " + coc);
		}
		this.modeCombinations = ModeCombinations.fromGlobalOptimizationContext(goc);
		this.normalizedEshModePreferenceRanks = GocUtils.normalizeEshModePreferenceRanks(goc.eshsWithDifferentModes());
	}

	protected int getTotalNumberOfSimulations() {
		return this.simulationsCounter.get();
	}

	protected int getTotalNumberOfGenerations() {
		return (int) this.generationsCounter.get();
	}

	protected static Fitness simulate(//
			GlobalOptimizationContext goc, //
			ModeCombinations modeCombinations, //
			int[] schedule, //
			BestScheduleCollector bsc, //
			List<Map<Integer, Double>> normalizedEshModePreferenceRanks) {
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
			simulatePeriod(gsc, cscs, periodIndex, modeCombination, fitness, bsc);
		}

		final var modePreferencePenalty = calculateModePreferencePenalty(goc, modeCombinations, schedule,
				normalizedEshModePreferenceRanks);
		fitness.setModePreferencePenalty(modePreferencePenalty);

		final var runLengthCost = calculateRunLengthCost(goc, modeCombinations, schedule);
		fitness.addSoftConstraintViolation(runLengthCost);

		return fitness;
	}

	/**
	 * Calculates the cost of one Period under the given Schedule.
	 * 
	 * @param gsc             the {@link GlobalScheduleContext}
	 * @param cscs            the ControllerScheduleContexts
	 * @param periodIndex     the index of the simulated period
	 * @param modeCombination the {@link ModeCombination} of the simulated period
	 * @param fitness         the {@link Fitness} result
	 * @param bsc             the {@link BestScheduleCollector}; or null
	 */
	public static void simulatePeriod(//
			GlobalScheduleContext gsc, //
			ImmutableMap<EnergyScheduleHandler, Object> cscs, //
			int periodIndex, //
			ModeCombination modeCombination, //
			Fitness fitness, //
			BestScheduleCollector bsc) {
		final var period = gsc.goc.periods().get(periodIndex);
		final var eshs = gsc.goc.eshs();

		final EnergyFlow.Model ef;
		try {
			ef = EnergyFlow.Model.from(gsc, period);
		} catch (OpenemsException e) {
			LOG.error("Error while simulating period [" + periodIndex + "]: " + e.getMessage());
			fitness.addHardConstraintViolation();
			return;
		}

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

					if (bsc == null) {
						e.simulate(period, gsc, csc, ef, preProcessedMode, fitness, false);

					} else {
						// Final run, collecting BestSchedule
						final var postProcessedMode = e.simulate(period, gsc, csc, ef, preProcessedMode, fitness, true);
						bsc.addMode(period.time(), e, postProcessedMode);
					}
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

		// Evaluate Grid-Buy Soft-Limit
		if (period.gridBuySoftLimit() != null && energyFlow.getGrid() > period.gridBuySoftLimit()) {
			fitness.addHardConstraintViolation();
		}

		if (period instanceof Period.WithPrice periodWithPrice) {
			final var price = periodWithPrice.price().actual();

			// Calculate Grid-Buy Cost
			if (energyFlow.getGrid() > 0) {
				int buyFromGrid = max(0, energyFlow.getGrid());
				int chargeEss = max(0, -energyFlow.getEss());
				int gridToEss = Math.min(buyFromGrid, chargeEss);
				int gridToCons = buyFromGrid - gridToEss;
				fitness.addGridBuyCost(
						// Cost for direct Consumption
						gridToCons * price
								// Cost for future Consumption after storage
								+ max(0, gridToEss) * price * EFFICIENCY_FACTOR);
			}

			// Calculate Grid-Sell Revenue
			if (energyFlow.getGrid() < 0) {
				int sellToGrid = max(0, -energyFlow.getGrid());
				int dischargeEnergy = max(0, energyFlow.getEss());
				int essToGrid = Math.min(sellToGrid, dischargeEnergy);
				fitness.addGridSellRevenue(//
						// Revenue for Discharge-to-Grid
						essToGrid * price);
			}
		}

		if (bsc != null) {
			bsc.addPeriod(period.time(),
					SimulationResult.Period.from(period, modeCombination, energyFlow, gsc.ess.getInitialEnergy()));
		}

		// Prepare for next period
		gsc.ess.calculateInitialEnergy(energyFlow.getEss());
	}

	/**
	 * Runs the optimization and reports the "best" simulation result.
	 *
	 * @param previousResultSupplier     supplies the previous
	 *                                   {@link SimulationResult}
	 * @param optimizeCurrentPeriod      whether the current period should be
	 *                                   optimized; if {@code false}, the
	 *                                   {@link Gene} of the current period is fixed
	 *                                   to the previous result
	 * @param engineInterceptor          interceptor to customize the
	 *                                   {@link Engine.Builder}
	 * @param evolutionStreamInterceptor interceptor to customize the
	 *                                   {@link EvolutionStream}
	 * @param onBestResult               consumer that is called to apply a new best
	 *                                   {@link SimulationResult}
	 */
	public void runOptimization(//
			Supplier<SimulationResult> previousResultSupplier, //
			boolean optimizeCurrentPeriod,
			Function<Engine.Builder<IntegerGene, Fitness>, Engine.Builder<IntegerGene, Fitness>> engineInterceptor, //
			Function<EvolutionStream<IntegerGene, Fitness>, EvolutionStream<IntegerGene, Fitness>> evolutionStreamInterceptor, //
			Consumer<SimulationResult> onBestResult) {
		final var isCurrentPeriodFixed = new AtomicBoolean(!optimizeCurrentPeriod);
		final var codec = EshCodec.of(this.goc, this.modeCombinations, previousResultSupplier,
				isCurrentPeriodFixed::get);
		if (codec == null) {
			// TODO if there are ESHs we should return the fixed Schedule as
			// SimulationResult
			onBestResult.accept(EMPTY_SIMULATION_RESULT);
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
					return simulate(this.goc, this.modeCombinations, gt, null, this.normalizedEshModePreferenceRanks);
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

		final var bestPt = new AtomicReference<Phenotype<IntegerGene, Fitness>>();
		final var earliestCallback = Instant.now().plus(this.earliestCallbackDelay);

		// Start the evaluation
		stream.forEach(er -> {
			this.generationsCounter.set(er.generation());
			var currentBest = er.bestPhenotype();

			// Update best phenotype
			bestPt.updateAndGet(prev -> {
				if (prev == null || currentBest.fitness().compareTo(prev.fitness()) < 0) {
					return currentBest;
				}
				return prev;
			});

			// Apply current best result
			if (!isCurrentPeriodFixed.get() && Instant.now().isAfter(earliestCallback)) {
				if (bestPt.get() == null) {
					onBestResult.accept(SimulationResult.EMPTY_SIMULATION_RESULT);
				} else {
					onBestResult.accept(SimulationResult.fromQuarters(//
							this.goc, //
							codec.decode(bestPt.get().genotype()), //
							this.getTotalNumberOfSimulations(), //
							this.getTotalNumberOfGenerations()));
				}
				// Fix current period form now on
				isCurrentPeriodFixed.set(true);
			}
		});

		// Apply final best result
		if (Instant.now().isAfter(earliestCallback)) {
			if (bestPt.get() == null) {
				onBestResult.accept(SimulationResult.EMPTY_SIMULATION_RESULT);
				return;
			}
			onBestResult.accept(SimulationResult.fromQuarters(//
					this.goc, //
					codec.decode(bestPt.get().genotype()), //
					this.getTotalNumberOfSimulations(), //
					this.getTotalNumberOfGenerations()));
		}
	}

	/**
	 * Calculates a weighted penalty based on the normalized preference ranks of
	 * modes in a given schedule.
	 *
	 * <p>
	 * The penalty is computed by summing the normalized preference scores of all
	 * modes for each ESH in every period. Early periods are given a higher weight,
	 * so that preferred modes appearing earlier contribute more to the total
	 * penalty.
	 *
	 * <p>
	 * Lower penalty values correspond to schedules that better match the preferred
	 * modes across ESHs and periods.
	 *
	 * @param goc                              the {@link GlobalOptimizationContext}
	 * @param modeCombinations                 the {@link ModeCombinations}
	 * @param schedule                         the given schedule
	 * @param normalizedEshModePreferenceRanks precomputed normalized preference
	 *                                         ranks for each ESH's modes
	 * @return the total weighted penalty for the given schedule
	 */
	private static double calculateModePreferencePenalty(GlobalOptimizationContext goc,
			ModeCombinations modeCombinations, int[] schedule,
			List<Map<Integer, Double>> normalizedEshModePreferenceRanks) {
		final int numPeriods = goc.periods().size();

		double penalty = 0.;
		for (int periodIndex = 0; periodIndex < numPeriods; periodIndex++) {
			final var modeCombination = modeCombinations.get(schedule[periodIndex]);

			double prefRankSum = 0.;
			for (int eshIndex = 0; eshIndex < goc.eshsWithDifferentModes().size(); eshIndex++) {
				if (!goc.eshsWithDifferentModes().get(eshIndex).modes().hasForOptimizer()) {
					continue;
				}
				prefRankSum += normalizedEshModePreferenceRanks.get(eshIndex)
						.get(modeCombination.mode(eshIndex).index());
			}

			final double weight = numPeriods - periodIndex + 1;
			penalty += weight * prefRankSum;
		}

		return penalty;
	}

	/**
	 * Calculates the run-length based cost for a schedule of modes.
	 * 
	 * <p>
	 * Schedules that frequently change modes incur a higher cost, whereas schedules
	 * that maintain the same mode across multiple consecutive periods are rewarded
	 * with a lower cost. The cost decreases non-linearly with the length of
	 * consecutive identical modes, encouraging longer uninterrupted sequences.
	 * 
	 * <p>
	 * Example:
	 * 
	 * <pre>
	 * Modes: [A, A, A, B, B, C]
	 * Runs:  AAA, BB, C
	 * Cost:  1/3^2 + 1/2^2 + 1/1^2 = 1/9 + 1/4 + 1 = 1.3611
	 * </pre>
	 * </p>
	 * 
	 * @param goc              the {@link GlobalOptimizationContext}
	 * @param modeCombinations the {@link ModeCombinations}
	 * @param schedule         the Schedule
	 * @return the run-length cost; smaller is better
	 */
	private static int calculateRunLengthCost(GlobalOptimizationContext goc, ModeCombinations modeCombinations,
			int[] schedule) {
		final var noOfPeriods = goc.periods().size();
		float cost = 0.0F;
		for (var eshIndex = 0; eshIndex < goc.eshsWithDifferentModes().size(); eshIndex++) {
			int runLength = 1;
			var lastMode = modeCombinations.get(schedule[0]).mode(eshIndex);
			for (var periodIndex = 1; periodIndex < noOfPeriods; periodIndex++) {
				var thisMode = modeCombinations.get(schedule[periodIndex]).mode(eshIndex);
				if (thisMode.equals(lastMode)) {
					runLength++;
				} else {
					cost += 1.0F / (runLength * runLength);
					runLength = 1;
				}
				lastMode = thisMode;
			}
			cost += 1.0F / (runLength * runLength);
		}

		return Math.round(cost);
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

	@VisibleForTesting
	public void setEarliestCallbackDelay(Duration delay) {
		this.earliestCallbackDelay = delay;
	}
}
