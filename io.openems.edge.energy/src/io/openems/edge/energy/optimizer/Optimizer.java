package io.openems.edge.energy.optimizer;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.jenetics.engine.Limits.byExecutionTime;
import static io.jenetics.engine.Limits.byFixedGeneration;
import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.common.utils.ThreadPoolUtils.shutdownAndAwaitTermination;
import static io.openems.edge.energy.optimizer.SimulationResult.EMPTY_SIMULATION_RESULT;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.calculateSleepMillis;
import static io.openems.edge.energy.optimizer.Utils.createSimulator;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.lang.Thread.sleep;
import static java.time.Duration.ofSeconds;

import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.Optional;
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
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.api.LogVerbosity;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler.Fitness;
import io.openems.edge.energy.api.handler.OneMode;
import io.openems.edge.energy.api.simulation.GlobalOptimizationContext;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer implements Runnable {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);
	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

	private final Supplier<LogVerbosity> logVerbosity;
	private final Supplier<GlobalOptimizationContext> gocSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;
	private final AtomicBoolean rescheduleCurrentPeriod = new AtomicBoolean(false);

	private Simulator simulator = null;
	private SimulationResult simulationResult = EMPTY_SIMULATION_RESULT;
	private ScheduledFuture<?> future;

	public Optimizer(Supplier<LogVerbosity> logVerbosity, Supplier<GlobalOptimizationContext> gocSupplier, //
			Channel<Integer> simulationsPerQuarterChannel) {
		this.logVerbosity = logVerbosity;
		this.gocSupplier = gocSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		initializeRandomRegistryForProduction();
	}

	/**
	 * Interrupts the {@link Optimizer} Task.
	 */
	public synchronized void interruptTask() {
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
	 * 
	 * @param reason a reason
	 */
	public void triggerReschedule(String reason) {
		// NOTE: This is what happens here:
		// [_cycle ] INFO [dge.energy.optimizer.Optimizer] OPTIMIZER Trigger Reschedule.
		// Reason: ControllerEvcsImpl::onEvcsStatusChange from 6:The charging limit
		// reached to 1:Not ready for Charging
		// [thread-1] INFO [dge.energy.optimizer.Optimizer] OPTIMIZER Optimizer::run()
		// InterruptedException: null
		// [thread-1] INFO [dge.energy.optimizer.Optimizer] OPTIMIZER Simulation gave no
		// result!
		// [thread-1] INFO [dge.energy.optimizer.Optimizer] OPTIMIZER Run Quick
		// Optimization...
		// [thread-1] INFO [dge.energy.optimizer.Optimizer] OPTIMIZER
		// updateSimulator()...

		// TODO On interrupt: keep best "regularOptimization" up till now as input for
		// next InitialPopulation
		this.traceLog(() -> "Trigger Reschedule. Reason: " + reason);
		this.rescheduleCurrentPeriod.set(true);
		this.activate(); // interrupt + reschedule
	}

	@Override
	public void run() {
		var simulationResult = EMPTY_SIMULATION_RESULT;
		try {
			if (this.rescheduleCurrentPeriod.getAndSet(false) || this.simulationResult == EMPTY_SIMULATION_RESULT) {
				this.traceLog(() -> "Run Quick Optimization...");
				simulationResult = this.runQuickOptimization();
			} else {
				this.traceLog(() -> "Run Regular Optimization...");
				simulationResult = this.runRegularOptimization();
			}

		} catch (InterruptedException e) {
			this.traceLog(() -> "Optimizer::run() " + e.getClass().getSimpleName() + ": " + e.getMessage());
			Thread.currentThread().interrupt(); // reset interrupt status
		} catch (ExecutionException | IllegalArgumentException e) {
			this.traceLog(() -> "Optimizer::run() " + e.getClass().getSimpleName() + ": " + e.getMessage());
		}

		this.applySimulationResult(simulationResult);
	}

	/**
	 * Creates a new {@link Simulator} using the `gscSupplier` and updates
	 * `this.simulator`.
	 * 
	 * @throws InterruptedException on interrupted sleep
	 */
	private synchronized void updateSimulator() throws InterruptedException {
		try {
			// Create the Simulator with GlobalOptimizationContext
			this.traceLog(() -> "updateSimulator()...");
			final var simulator = createSimulator(this.gocSupplier, //
					error -> {
						this.traceLog(error);
						this.applySimulationResult(EMPTY_SIMULATION_RESULT);
					});
			if (simulator == null) {
				this.traceLog(() -> "Simulator is null");
			} else {
				this.traceLog(() -> "Simulator is " + simulator.toJson().toString());
			}
			this.simulator = simulator;

		} catch (Exception e) {
			this.simulator = null;
			this.traceLog(() -> "Unable to update Simulator: " + e.getMessage());
			e.printStackTrace(); // TODO remove
			throw e;
		}
	}

	/**
	 * Runs a quick optimization with only one generation.
	 * 
	 * @return a {@link SimulationResult} or null
	 * @throws InterruptedException on interrupted sleep
	 * @throws ExecutionException   on simulation error
	 */
	protected synchronized SimulationResult runQuickOptimization() throws InterruptedException, ExecutionException {
		if (this.simulationResult == EMPTY_SIMULATION_RESULT) {
			this.traceLog(() -> "reschedule because previous simulationresult is EMPTY");
		} else {
			this.traceLog(() -> "triggerReschedule() had been called -> reschedule");
		}

		this.updateSimulator();
		return this.runSimulation(//
				false, // current period can get adjusted
				byFixedGeneration(1)) // simulate only two generations
				.get();
	}

	/**
	 * Runs a regular optimization for upcoming periods.
	 * 
	 * @return a {@link SimulationResult} or null
	 * @throws InterruptedException on interrupted sleep
	 * @throws ExecutionException   on simulation error
	 */
	protected synchronized SimulationResult runRegularOptimization() throws InterruptedException, ExecutionException {
		// Run regular optimization for upcoming periods
		var millisTillNextQuarter = calculateSleepMillis();
		if (millisTillNextQuarter < 60_000 /* 60s */) {
			this.traceLog(() -> "Run Simulation in " + millisTillNextQuarter + "ms...");
			sleep(millisTillNextQuarter);
		}

		this.updateSimulator();
		return this.runSimulation(//
				true, // current period should not get adjusted
				byExecutionTime(ofSeconds(calculateExecutionLimitSeconds()))) // Limit by execution time
				.get();
	}

	protected CompletableFuture<SimulationResult> runSimulation(boolean isCurrentPeriodFixed,
			Predicate<? super EvolutionResult<IntegerGene, Fitness>> executionLimit) {
		if (this.simulator == null) {
			return CompletableFuture.completedFuture(EMPTY_SIMULATION_RESULT);
		}

		this.traceLog(() -> "Run next Simulation");
		return CompletableFuture.supplyAsync(() -> {
			this.traceLog(() -> "Executing async Simulation");

			var bestSchedule = this.simulator.getBestSchedule(this.simulationResult, isCurrentPeriodFixed, null, //
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
		if (simulationResult == EMPTY_SIMULATION_RESULT /* no result */) {
			this.traceLog(() -> "Simulation gave no result!");
		}

		Optional.ofNullable(this.simulator).ifPresent(simulator -> {
			// Debug Log best Schedule
			logSimulationResult(simulator, simulationResult);
			this.simulationsPerQuarterChannel.setNextValue(simulator.getTotalNumberOfSimulations());
		});

		// Store result
		this.simulationResult = simulationResult;

		// Send Schedule to EnergyScheduleHandlers.WithDifferentModes
		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule);
		});
		// Send Schedule to EnergyScheduleHandlers.WithOnlyOneMode
		var schedule = simulationResult.periods().entrySet().stream() //
				.collect(toImmutableSortedMap(ZonedDateTime::compareTo, //
						Entry::getKey, //
						e -> new OneMode.Period.Transition(//
								e.getValue().period().duration(), //
								e.getValue().period().price(), //
								e.getValue().energyFlow())));
		simulationResult.eshsWithOnlyOneMode().forEach(esh -> {
			esh.applySchedule(schedule);
		});
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
			b.append("ScheduledPeriods:").append(this.simulationResult.periods().size());
		}
		b.append("|SimulationsPerQuarter:").append(this.simulationsPerQuarterChannel.value());
		Optional.ofNullable(this.simulator).ifPresent(simulator -> {
			b.append("|Current:").append(simulator.getTotalNumberOfSimulations());
		});

		return b.toString();
	}
}
