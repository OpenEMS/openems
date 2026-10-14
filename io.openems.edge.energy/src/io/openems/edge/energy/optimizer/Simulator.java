package io.openems.edge.energy.optimizer;

import static io.openems.common.utils.JsonUtils.buildJsonObject;
import static io.openems.edge.energy.optimizer.InitialPopulationUtils.generateInitialPopulation;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static java.lang.Math.max;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonObject;

import io.jenetics.EliteSelector;
import io.jenetics.IntegerGene;
import io.jenetics.Mutator;
import io.jenetics.Phenotype;
import io.jenetics.ShiftMutator;
import io.jenetics.SinglePointCrossover;
import io.jenetics.TournamentSelector;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionStream;
import io.jenetics.engine.Limits;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.energy.api.handler.AbstractEnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.Fitness;
import io.openems.edge.energy.api.simulation.EnergyFlow;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;
import io.openems.edge.energy.api.simulation.GlobalScheduleContext;
import io.openems.edge.energy.api.simulation.GocUtils;
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

	private Duration publishInterval = Duration.ofSeconds(30);

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

	protected static Fitness.Builder simulate(//
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
		final var fitness = Fitness.builder();

		for (var periodIndex = 0; periodIndex < noOfPeriods; periodIndex++) {
			var modeCombination = modeCombinations.get(schedule[periodIndex]);
			simulatePeriod(gsc, cscs, periodIndex, modeCombination, fitness, bsc);
		}

		final var modePreferencePenalty = calculateModePreferencePenalty(goc, modeCombinations, schedule,
				normalizedEshModePreferenceRanks);
		fitness.withModePreferencePenalty(modePreferencePenalty);

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
	 * @param fitness         the {@link Fitness.Builder} result
	 * @param bsc             the {@link BestScheduleCollector}; or null
	 */
	public static void simulatePeriod(//
			GlobalScheduleContext gsc, //
			ImmutableMap<EnergyScheduleHandler, Object> cscs, //
			int periodIndex, //
			ModeCombination modeCombination, //
			Fitness.Builder fitness, //
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

		// Simulate period
		int modeIndex = 0;
		for (var esh : eshs) {
			try {
				var csc = cscs.get(esh);
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentModes e -> {
					final int eshModeIndex = resolveModeIndex(e, modeCombination, modeIndex++);
					final int preProcessedMode = e.preProcessPeriod(period, gsc, eshModeIndex);

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

		// Evaluate period
		modeIndex = 0;
		for (var esh : eshs) {
			try {
				var csc = cscs.get(esh);
				switch (esh) {
				case EnergyScheduleHandler.WithDifferentModes e -> {
					final int eshModeIndex = resolveModeIndex(e, modeCombination, modeIndex++);
					final var preProcessedMode = e.preProcessPeriod(period, gsc, eshModeIndex);
					e.evaluate(period, gsc, csc, energyFlow, preProcessedMode, fitness, bsc != null);
				}
				case EnergyScheduleHandler.WithOnlyOneMode e -> //
					e.evaluate(period, gsc, csc, energyFlow, fitness);
				}
			} catch (RuntimeException e) {
				throw new RuntimeException("Error during evaluation of [" + esh.getParentId() + "] " //
						+ "Period [" + period.index() + "/" + period.time() + "]: " + e.getMessage(), e);
			}
		}

		// Evaluate Grid-Buy Soft-Limit
		if (period.gridBuySoftLimit() != null && energyFlow.getGrid() > period.gridBuySoftLimit()) {
			fitness.addHardConstraintViolation();
		}

		// Calculate Grid-Buy metrics
		if (energyFlow.getGrid() > 0) {
			final int buyFromGrid = max(0, energyFlow.getGrid());
			final int chargeEss = max(0, -energyFlow.getEss());
			final int gridToEss = Math.min(buyFromGrid, chargeEss);
			final int gridToCons = buyFromGrid - gridToEss;

			period.data().gridBuyPrice().ifPresent(p -> {
				final double shiftedGridBuyPrice = p.positiveShifted();

				// Cost for direct consumption
				final double directCost = gridToCons * shiftedGridBuyPrice;
				// Cost for future consumption after storage
				final double costWithStorage = gridToEss * shiftedGridBuyPrice * EFFICIENCY_FACTOR;

				fitness.addGridBuyCostScore(directCost + costWithStorage);
			});

			fitness.addGridBuyEnergyWh(
					// Energy for direct consumption
					gridToCons
							// Energy for future consumption after storage
							+ gridToEss * EFFICIENCY_FACTOR);
		}

		// Calculate Grid-Sell metrics
		if (energyFlow.getGrid() < 0) {
			final int sellToGrid = max(0, -energyFlow.getGrid());

			period.data().gridSellPrice().ifPresent(p -> {
				final double gridSellPrice = p.actual();
				fitness.addGridSellRevenueScore(sellToGrid * gridSellPrice);
			});

			fitness.addGridSellEnergyWh(sellToGrid);
		}

		if (bsc != null) {
			bsc.addPeriod(period.time(),
					SimulationResult.Period.from(period, modeCombination, energyFlow, gsc.ess.getInitialEnergy()));
		}

		// Prepare for next period
		gsc.ess.calculateInitialEnergy(energyFlow.getEss());
	}

	/**
	 * Runs the optimization and reports intermediate and final optimization
	 * results.
	 *
	 * @param previousResultSupplier       supplies the previously applied
	 *                                     {@link SimulationResult}
	 * @param isCurrentPeriodFixedSupplier supplies whether the current period is
	 *                                     fixed to the value of the previously
	 *                                     applied result
	 * @param engineInterceptor            interceptor to customize the
	 *                                     {@link Engine.Builder}
	 * @param evolutionStreamInterceptor   interceptor to customize the
	 *                                     {@link EvolutionStream}
	 * @param onResult                     consumer that is called with intermediate
	 *                                     and final {@link OptimizationResult}s
	 */
	public void runOptimization(//
			Supplier<SimulationResult> previousResultSupplier, //
			BooleanSupplier isCurrentPeriodFixedSupplier, //
			Function<Engine.Builder<IntegerGene, Fitness>, Engine.Builder<IntegerGene, Fitness>> engineInterceptor, //
			Function<EvolutionStream<IntegerGene, Fitness>, EvolutionStream<IntegerGene, Fitness>> evolutionStreamInterceptor, //
			Consumer<OptimizationResult> onResult) {
		if (this.goc.eshsWithDifferentModes().isEmpty()) {
			this.simulationsCounter.incrementAndGet();
			onResult.accept(new OptimizationResult(//
					SimulationResult.fromQuarters(//
							this.goc, //
							new int[this.goc.periods().size()], //
							this.getTotalNumberOfSimulations(), //
							this.getTotalNumberOfGenerations()), //
					OptimizationResult.Type.FINAL));
			return;
		}

		final var codec = EshCodec.of(//
				this.goc, //
				this.modeCombinations, //
				previousResultSupplier, //
				isCurrentPeriodFixedSupplier);

		if (codec == null) {
			onResult.accept(new OptimizationResult(//
					EMPTY_SIMULATION_RESULT, //
					OptimizationResult.Type.FINAL));
			return;
		}

		final var executor = createExecutor();

		try {
			var engine = this.buildEngine(codec, executor);
			if (engineInterceptor != null) {
				engine = engineInterceptor.apply(engine);
			}

			var stream = engine.build()//
					.stream(generateInitialPopulation(codec))//
					.limit(Limits.bySteadyFitness(500))//
					.limit(result -> !Thread.currentThread().isInterrupted());

			if (evolutionStreamInterceptor != null) {
				stream = evolutionStreamInterceptor.apply(stream);
			}

			final var state = new RunState(this.publishInterval);

			stream.forEach(er -> {
				this.generationsCounter.set(er.generation());

				final var currentBest = er.bestPhenotype();
				if (isBetter(currentBest, state.best)) {
					state.best = currentBest;
				}

				if (this.shouldPublish(state)) {
					onResult.accept(this.toOptimizationResult(//
							codec, //
							state.best, //
							OptimizationResult.Type.INTERMEDIATE));

					state.lastPublished = state.best;
				}
			});

			final var best = state.best;
			onResult.accept(best == null //
					? new OptimizationResult(EMPTY_SIMULATION_RESULT, OptimizationResult.Type.FINAL) //
					: this.toOptimizationResult(codec, best, OptimizationResult.Type.FINAL));

		} finally {
			shutdownExecutor(executor);
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

	private static int resolveModeIndex(//
			EnergyScheduleHandler.WithDifferentModes esh, //
			ModeCombination modeCombination, //
			int index) {
		if (esh.modes().isEmpty()) {
			return -1;
		}

		return Optional.ofNullable(modeCombination.mode(index))//
				.map(ModeCombinations.Mode::index)//
				.orElse(-1);
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

	private static Executor createExecutor() {
		final var threadCount = Math.max(1, Runtime.getRuntime().availableProcessors() - 1);

		if (threadCount == 1) {
			return Runnable::run;
		}

		return Executors.newFixedThreadPool(//
				threadCount, //
				new OptimizerThreadFactory(//
						OptimizerThreadFactory.KEY_JENETICS, //
						Thread.MIN_PRIORITY));
	}

	private Engine.Builder<IntegerGene, Fitness> buildEngine(//
			EshCodec codec, //
			Executor executor) {
		return Engine //
				.builder(gt -> {
					this.simulationsCounter.incrementAndGet();

					return simulate(//
							this.goc, //
							this.modeCombinations, //
							gt, //
							null, //
							this.normalizedEshModePreferenceRanks) //
							.build();
				}, codec) //
				.selector(new EliteSelector<IntegerGene, Fitness>(new TournamentSelector<>())) //
				.alterers(//
						new Mutator<>(0.05), //
						new ShiftMutator<>(0.2), //
						new SinglePointCrossover<>(0.2)) //
				.populationSize(300) //
				.executor(executor) //
				.minimizing();
	}

	private static boolean isBetter(//
			Phenotype<IntegerGene, Fitness> candidate, //
			Phenotype<IntegerGene, Fitness> reference) {
		return reference == null //
				|| candidate.fitness().compareTo(reference.fitness()) < 0;
	}

	private boolean shouldPublish(RunState state) {
		if (state.best == null) {
			return false;
		}

		final var now = Instant.now(this.goc.clock());
		if (now.isBefore(state.nextPublishAt)) {
			return false;
		}

		state.nextPublishAt = now.plus(this.publishInterval);

		return isBetter(state.best, state.lastPublished);
	}

	private OptimizationResult toOptimizationResult(//
			EshCodec codec, //
			Phenotype<IntegerGene, Fitness> phenotype, //
			OptimizationResult.Type type) {
		return new OptimizationResult(//
				SimulationResult.fromQuarters(//
						this.goc, //
						codec.decode(phenotype.genotype()), //
						this.getTotalNumberOfSimulations(), //
						this.getTotalNumberOfGenerations()), //
				type);
	}

	private static void shutdownExecutor(Executor executor) {
		if (executor instanceof ThreadPoolExecutor poolExecutor) {
			poolExecutor.shutdownNow();
		}
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
	public void setPublishInterval(Duration interval) {
		this.publishInterval = interval;
	}

	private final class RunState {

		private Phenotype<IntegerGene, Fitness> best;
		private Phenotype<IntegerGene, Fitness> lastPublished;
		private Instant nextPublishAt;

		private RunState(Duration publishInterval) {
			this.nextPublishAt = Instant.now(Simulator.this.goc.clock()).plus(publishInterval);
		}
	}
}
