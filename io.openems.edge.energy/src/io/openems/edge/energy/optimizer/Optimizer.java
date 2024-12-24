package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.calculateSleepMillis;
import static io.openems.edge.energy.optimizer.Utils.createSimulator;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jenetics.IntegerGene;
import io.jenetics.engine.EvolutionResult;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.LogVerbosity;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer implements Runnable {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private final Supplier<LogVerbosity> logVerbosity;
	private final ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;
	private final AtomicBoolean rescheduleCurrentPeriod = new AtomicBoolean(false);

	private Simulator simulator = null;
	private SimulationResult simulationResult = EMPTY;
	private ScheduledFuture<?> future;

	public Optimizer(Supplier<LogVerbosity> logVerbosity,
			ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier, //
			Channel<Integer> simulationsPerQuarterChannel) {
		this.logVerbosity = logVerbosity;
		this.gscSupplier = gscSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		initializeRandomRegistryForProduction();
	}

	private synchronized void interruptTask() {
		if (this.future != null) {
			this.future.cancel(true);
		}
	}

	/**
	 * Activate and start the {@link Optimizer}.
	 */
	public synchronized void activate() {
		this.interruptTask();
		this.future = this.executor.scheduleAtFixedRate(this, 0, 1, TimeUnit.SECONDS);
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public synchronized void deactivate() {
		this.interruptTask();
		shutdownAndAwaitTermination(this.executor, 0);
	}

	/**
	 * Triggers Rescheduling.
	 */
	public void triggerReschedule() {
		this.traceLog(() -> "Trigger Reschedule");
		this.activate(); // interrupt + reschedule
		this.rescheduleCurrentPeriod.set(true);
	}

	@Override
	public void run() {
		SimulationResult simulationResult = SimulationResult.EMPTY;
		try {
			this.traceLog(() -> "Run...");

			if (this.rescheduleCurrentPeriod.getAndSet(false) || this.simulationResult == EMPTY) {
				simulationResult = this.runQuickOptimization();
			} else {
				simulationResult = this.runRegularOptimization();
			}

		} catch (InterruptedException | ExecutionException | IllegalArgumentException e) {
			this.traceLog(() -> "Optimizer::run() " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		this.applySimulationResult(simulationResult);
	}

	/**
	 * Creates a new {@link Simulator} using the `gscSupplier` and updates
	 * `this.simulator`.
	 * 
	 * @return a {@link Simulator} or null
	 * @throws InterruptedException on interrupted sleep
	 */
	private Simulator updateSimulator() throws InterruptedException {
		// Create the Simulator with GlobalSimulationsContext
		createSimulator(this.gscSupplier, //
				simulator -> this.simulator = simulator, //
				error -> {
					this.traceLog(error);
					this.applySimulationResult(EMPTY);
				});
		final var simulator = this.simulator;
		if (simulator == null) {
			this.traceLog(() -> "Simulator is null");
		} else {
			this.traceLog(() -> "Simulator is " + simulator.toLogString(""));
		}
		return simulator;
	}

	/**
	 * Runs a quick optimization with only one generation.
	 * 
	 * @return a {@link SimulationResult} or null
	 * @throws InterruptedException on interrupted sleep
	 * @throws ExecutionException   on simulation error
	 */
	protected SimulationResult runQuickOptimization() throws InterruptedException, ExecutionException {
		var simulator = this.updateSimulator();
		if (simulator == null) {
			return SimulationResult.EMPTY;
		}

		if (this.simulationResult == EMPTY) {
			this.traceLog(() -> "reschedule because previous simulationresult is EMPTY");
		} else {
			this.traceLog(() -> "triggerReschedule() had been called -> reschedule");
		}
		return this.runSimulation(simulator, //
				false, // current period can get adjusted
				byFixedGeneration(1)) // simulate only one generation
				.get();
	}

	/**
	 * Runs a regular optimization for upcoming periods.
	 * 
	 * @return a {@link SimulationResult} or null
	 * @throws InterruptedException on interrupted sleep
	 * @throws ExecutionException   on simulation error
	 */
	protected SimulationResult runRegularOptimization() throws InterruptedException, ExecutionException {
		// Run regular optimization for upcoming periods
		var millisTillNextQuarter = calculateSleepMillis();
		if (millisTillNextQuarter < 60_000 /* 60s */) {
			this.traceLog(() -> "Run Simulation in " + millisTillNextQuarter + "ms...");
			sleep(millisTillNextQuarter);
		}
		var simulator = this.updateSimulator();
		if (simulator == null) {
			return SimulationResult.EMPTY;
		}

		this.traceLog(() -> "Run Simulation");
		return this.runSimulation(simulator, //
				true, // current period should not get adjusted
				byExecutionTime(ofSeconds(calculateExecutionLimitSeconds()))) // Limit by execution time
				.get();
	}

	protected CompletableFuture<SimulationResult> runSimulation(Simulator simulator, boolean isCurrentPeriodFixed,
			Predicate<? super EvolutionResult<IntegerGene, Double>> executionLimit) {
		this.traceLog(() -> "Run next Simulation");
		return CompletableFuture.supplyAsync(() -> {
			this.traceLog(() -> "Executing async Simulation");

			var bestSchedule = simulator.getBestSchedule(this.simulationResult, isCurrentPeriodFixed, null, //
					stream -> stream //
							// Stop till next quarter
							.limit(executionLimit));

			return bestSchedule;
		});
	}

	/**
	 * Applies the Schedule to all {@link EnergyScheduleHandler}s and stores the
	 * {@link SimulationResult} in `this.simulationResult`.
	 *
	 * @param simulationResult the {@link SimulationResult}
	 */
	protected void applySimulationResult(SimulationResult simulationResult) {
		if (simulationResult == EMPTY /* no result */) {
			this.traceLog(() -> "Simulation gave no result!");
		}

		final var simulator = this.simulator;
		if (simulator != null) {
			// Debug Log best Schedule
			logSimulationResult(simulator, simulationResult);

			// Calculate metrics
			var stats = simulator.cache.stats();
			this.simulationsPerQuarterChannel.setNextValue(stats.loadCount());
		}

		// Store result
		this.simulationResult = simulationResult;

		// Send Schedule to Controllers
		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule);
		});
	}

	private void traceLog(Supplier<String> message) {
		switch (this.logVerbosity.get()) {
		case NONE, DEBUG_LOG -> FunctionUtils.doNothing();
		case TRACE -> this.log.info("OPTIMIZER " + message.get());
		}
	}

	/**
	 * Gets the {@link SimulationResult}.
	 * 
	 * @return {@link SimulationResult}
	 */
	public SimulationResult getSimulationResult() {
		return this.simulationResult;
	}

	/**
	 * Output for Controller.Debug.Log.
	 *
	 * @return the debug log output
	 */
	public String debugLog() {
		var b = new StringBuilder();
		if (this.simulationResult.periods().isEmpty()) {
			b.append("No Schedule available");
		} else {
			b.append("ScheduledPeriods:" + this.simulationResult.periods().size());
		}
		var simulator = this.simulator;
		if (simulator != null) {
			var stats = simulator.cache.stats();
			b.append("|SimulationCounter:" + stats.loadCount());
		}
		b.append("|PerQuarter:" + this.simulationsPerQuarterChannel.value());
		return b.toString();
	}
}
