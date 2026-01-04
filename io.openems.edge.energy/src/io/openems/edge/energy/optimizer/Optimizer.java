package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.calculateSleepMillis;
import static io.openems.edge.energy.optimizer.Utils.createSimulator;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.time.Duration.ofSeconds;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.annotations.VisibleForTesting;

import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionResult;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * The optimization process is executed once in the beginning and afterwards
 * every full 15 minutes.
 */
public class Optimizer {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final Supplier<LogVerbosity> logVerbosity;
	private final Supplier<GlobalOptimizationContext> gocSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;

	private final AtomicReference<Simulator> simulator = new AtomicReference<>(null);
	private final AtomicReference<SimulationResult> simulationResult = new AtomicReference<>(EMPTY_SIMULATION_RESULT);

	private ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private CompletableFuture<SimulationResult> quickOptimizationFuture;
	private ScheduledFuture<?> regularOptimizationFuture;

	public Optimizer(//
			Supplier<LogVerbosity> logVerbosity, //
			Supplier<GlobalOptimizationContext> gocSupplier, //
			Channel<Integer> simulationsPerQuarterChannel) {
		this.logVerbosity = logVerbosity;
		this.gocSupplier = gocSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		initializeRandomRegistryForProduction();
	}

	/**
	 * Activate and start the {@link Optimizer}.
	 */
	public void activate() {
		this.traceLog(() -> "Activate");
		if (this.executor == null || this.executor.isShutdown() || this.executor.isTerminated()) {
			this.executor = Executors.newSingleThreadScheduledExecutor();
		}
		this.startOptimizationCycle();
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public void deactivate() {
		this.traceLog(() -> "Deactivate optimizer");
		this.interruptTask();
		if (this.executor != null) {
			this.executor.shutdownNow();
			this.executor = null;
		}
	}

	/**
	 * Interrupt the optimization task.
	 */
	public void interruptTask() {
		this.traceLog(() -> "Interrupt optimization task");
		if (this.quickOptimizationFuture != null) {
			this.quickOptimizationFuture.cancel(true);
		}
		if (this.regularOptimizationFuture != null) {
			this.regularOptimizationFuture.cancel(true);
		}
	}

	/**
	 * Trigger the rescheduling process.
	 *
	 * @param reason the reason why rescheduling is being triggered
	 */
	public void triggerReschedule(String reason) {
		// TODO: On interrupt, keep best 'regular optimization' achieved so far as the
		// input for the next initial population
		this.traceLog(() -> "Rescheduling triggered. Reason: " + reason);
		this.interruptTask();
		this.startOptimizationCycle();
	}

	private void startOptimizationCycle() {
		this.traceLog(() -> "Starting optimization cycle...");

		// Run quick optimization asynchronously
		this.quickOptimizationFuture = CompletableFuture.supplyAsync(this::runQuickOptimization, this.executor);

		this.quickOptimizationFuture.thenAccept(simResult -> {
			// Apply the simulation result
			this.applySimulationResult(simResult);

			if (simResult == EMPTY_SIMULATION_RESULT) {
				this.executor.schedule(//
						() -> this.triggerReschedule("Previous simulation result is empty"), //
						1, //
						TimeUnit.SECONDS);
			} else {
				this.scheduleRegularOptimization();
			}
		});
	}

	private void scheduleRegularOptimization() {
		// Schedule every 15 minutes after the previous completes
		this.regularOptimizationFuture = this.executor.scheduleWithFixedDelay(//
				this::runRegularOptimization, //
				0, // Initial delay
				15, // Delay
				TimeUnit.MINUTES);
	}

	@VisibleForTesting
	protected SimulationResult runQuickOptimization() {
		try {
			this.traceLog(() -> "Run quick optimization...");
			this.updateSimulator();
			return this.runSimulation(false, byFixedGeneration(1));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.traceLog(() -> "Quick optimization interrupted");
			return EMPTY_SIMULATION_RESULT;
		} catch (Exception e) {
			this.traceLog(() -> "Error during quick optimization: " + e.getMessage());
			e.printStackTrace();
			return EMPTY_SIMULATION_RESULT;
		}
	}

	private void runRegularOptimization() {
		try {
			this.traceLog(() -> "Run regular optimization...");
			var millisTillNextQuarter = calculateSleepMillis();
			if (millisTillNextQuarter < 60_000) {
				this.traceLog(() -> "Run simulation in " + millisTillNextQuarter + "ms...");
				Thread.sleep(millisTillNextQuarter);
			}
			this.updateSimulator();
			var simResult = this.runSimulation(//
					true, //
					byExecutionTime(ofSeconds(calculateExecutionLimitSeconds())));
			this.applySimulationResult(simResult);

			// Trigger rescheduling if empty simulation result
			if (simResult == EMPTY_SIMULATION_RESULT) {
				this.interruptTask();
				this.executor.schedule(//
						() -> this.triggerReschedule("Previous simulation result is empty"), //
						1, //
						TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.traceLog(() -> "Regular optimization interrupted");
		} catch (Exception e) {
			this.traceLog(() -> "Error during regular optimization: " + e.getMessage());
		}
	}

	/**
	 * Creates a new {@link Simulator} using the {@link #gocSupplier} and updates
	 * {@link #simulator}.
	 * 
	 * @throws InterruptedException if the thread is interrupted while creating the
	 *                              simulator
	 */
	private void updateSimulator() throws InterruptedException {
		this.traceLog(() -> "Updating simulator...");

		final var newSimulator = createSimulator(this.gocSupplier, error -> this.traceLog(error));
		if (newSimulator == null) {
			this.traceLog(() -> "Simulator is null");
		} else {
			this.traceLog(() -> "Simulator is " + newSimulator.toJson().toString());
		}

		this.simulator.set(newSimulator);
	}

	protected SimulationResult runSimulation(//
			boolean isCurrentPeriodFixed, //
			Predicate<? super EvolutionResult<IntegerGene, Fitness>> executionLimit) {
		var currentSimulator = this.simulator.get();
		if (currentSimulator == null) {
			return EMPTY_SIMULATION_RESULT;
		}
		this.traceLog(() -> "Running simulation...");
		return currentSimulator.getBestSchedule(//
				this.simulationResult.get(), //
				isCurrentPeriodFixed, //
				null, //
				stream -> stream.limit(executionLimit));
	}

	/**
	 * Applies the given {@link SimulationResult} to all relevant
	 * {@link EnergyScheduleHandler}s and stores it in {@link #simulationResult}.
	 *
	 * @param simulationResult the simulation result to apply
	 */
	protected void applySimulationResult(SimulationResult simulationResult) {
		Optional.ofNullable(this.simulator.get()).ifPresent(s -> {
			logSimulationResult(s, simulationResult);
			this.simulationsPerQuarterChannel.setNextValue(s.getTotalNumberOfSimulations());
		});

		this.simulationResult.set(simulationResult);

		// Apply schedules to EnergyScheduleHandlers.WithDifferentModes
		simulationResult.schedules().forEach((esh, schedule) -> esh.applySchedule(schedule));

		// Apply schedules to EnergyScheduleHandlers.WithOnlyOneMode
		var schedule = simulationResult.periods().entrySet().stream() //
				.collect(toImmutableSortedMap(//
						ZonedDateTime::compareTo, //
						Entry::getKey, //
						e -> {
							var p = e.getValue();
							var price = switch (p.period()) {
							case GlobalOptimizationContext.Period.WithPrice wp -> wp.price();
							default -> null;
							};
							return new OneMode.Period.Transition(p.period().duration(), price, p.energyFlow());
						}));

		simulationResult.eshsWithOnlyOneMode().forEach(esh -> esh.applySchedule(schedule));
	}

	private void traceLog(Supplier<String> message) {
		switch (this.logVerbosity.get()) {
		case NONE, DEBUG_LOG -> doNothing();
		case TRACE -> this.log.info("OPTIMIZER " + message.get());
		}
	}

	/**
	 * Gets the {@link SimulationResult}.
	 * 
	 * @return {@link SimulationResult}
	 */
	public SimulationResult getSimulationResult() {
		return this.simulationResult.get();
	}

	/**
	 * Output for Controller.Debug.Log.
	 *
	 * @return the debug log output
	 */
	public String debugLog() {
		var b = new StringBuilder();
		var currentResult = this.simulationResult.get();
		if (currentResult.periods().isEmpty()) {
			b.append("No Schedule available");
		} else {
			b.append("ScheduledPeriods:").append(currentResult.periods().size());
		}
		b.append("|SimulationsPerQuarter:").append(this.simulationsPerQuarterChannel.value());
		Optional.ofNullable(this.simulator.get()).ifPresent(simulator -> {
			b.append("|Current:").append(simulator.getTotalNumberOfSimulations());
		});
		return b.toString();
	}
}
