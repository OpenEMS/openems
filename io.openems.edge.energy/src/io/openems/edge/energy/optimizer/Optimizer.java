package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.energy.optimizer.QuickSchedules.findBestQuickSchedule;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.createSimulator;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.time.Duration.ofSeconds;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private final Supplier<LogVerbosity> logVerbosity;
	private final ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;
	private final AtomicBoolean interruptFlag = new AtomicBoolean(false);

	private Simulator simulator = null;
	private SimulationResult simulationResult = SimulationResult.EMPTY;

	public Optimizer(Supplier<LogVerbosity> logVerbosity,
			ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier, //
			Channel<Integer> simulationsPerQuarterChannel) {
		this.logVerbosity = logVerbosity;
		this.gscSupplier = gscSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		initializeRandomRegistryForProduction();
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public synchronized void deactivate() {
		this.interruptFlag.set(true);
	}

	/**
	 * Triggers Rescheduling.
	 */
	public void triggerReschedule() {
		this.traceLog(() -> "Trigger Reschedule");
		this.interruptFlag.set(true);
	}

	@Override
	public void run() {
		try {
			while (true) {
				this.traceLog(() -> "Run...");
				this.interruptFlag.set(false);

				// Create the Simulator with GlobalSimulationsContext
				createSimulator(this.gscSupplier, this.interruptFlag, //
						simulator -> this.simulator = simulator, //
						error -> {
							this.traceLog(error);
							this.applyEmptySimulationResult();
						});
				this.traceLog(() -> "Simulator is " + this.simulator);
				final var simulator = this.simulator;
				if (simulator == null) {
					continue;
				}

				this.runOnce(simulator);
			}
		} catch (InterruptedException | ExecutionException e) {
			this.log.error("OPTIMIZER execution failed InterruptedException|ExecutionException: " + e.getMessage());
			e.printStackTrace();

			// ignore
		} catch (Exception e) {
			this.log.error("OPTIMIZER execution failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	/**
	 * Run the optimization once.
	 * 
	 * @param simulator the {@link Simulator}
	 * @throws InterruptedException on error
	 * @throws ExecutionException   on error
	 */
	public void runOnce(Simulator simulator) throws InterruptedException, ExecutionException {
		if (this.simulationResult == SimulationResult.EMPTY) {
			// No Schedule available yet. Start with a default Schedule with all States
			// set to default.
			this.traceLog(() -> "No existing schedule available -> apply default");
			this.applyBestQuickSchedule(simulator);
		}

		this.traceLog(() -> "Run Simulation...");

		var simulationResult = this.runSimulation(simulator).get();
		if (simulationResult == null/* no result */ || this.interruptFlag.get() /* was interrupted */) {
			this.traceLog(() -> "Simulation gave no result or was interrupted!");
			this.simulationsPerQuarterChannel.setNextValue(null);
			this.applyBestQuickSchedule(simulator);
			return;
		}

		this.traceLog(() -> "Calculate metrics");

		// Calculate metrics
		var stats = simulator.cache.stats();
		this.simulationsPerQuarterChannel.setNextValue(stats.loadCount());

		// Apply simulation result to EnergyScheduleHandlers
		this.applySimulationResult(simulator, simulationResult, false);
	}

	private CompletableFuture<SimulationResult> runSimulation(Simulator simulator) {
		this.traceLog(() -> "Run next Simulation");
		return CompletableFuture.supplyAsync(() -> {
			this.traceLog(() -> "Executing async Simulation");

			final var executionLimit = byExecutionTime(ofSeconds(calculateExecutionLimitSeconds()));

			// Find best Schedule
			var bestSchedule = simulator.getBestSchedule(this.simulationResult, null, //
					stream -> stream //
							// Stop on interruptFlag
							.limit(ignore -> !this.interruptFlag.get()) //
							// Stop till next quarter
							.limit(executionLimit));

			return bestSchedule;
		});
	}

	/**
	 * Create and apply the best quickly available Schedule.
	 * 
	 * @param simulator the {@link Simulator}
	 */
	protected synchronized void applyBestQuickSchedule(Simulator simulator) {
		// Find Genotype with lowest cost
		var bestGt = findBestQuickSchedule(simulator, this.simulationResult);
		if (bestGt == null) {
			this.applyEmptySimulationResult();
			return;
		}
		var simulationResult = SimulationResult.fromQuarters(simulator.gsc, bestGt);

		this.traceLog(() -> "Applying best quick Schedule");
		this.applySimulationResult(simulator, simulationResult, true);
	}

	private void applyEmptySimulationResult() {
		this.traceLog(() -> "Applying empty Schedule");
		this.applySimulationResult(null, SimulationResult.EMPTY, true);
	}

	/**
	 * Applies the Schedule to all {@link EnergyScheduleHandler}s and stores the
	 * {@link SimulationResult} in `this.simulationResult`.
	 *
	 * @param simulator           the {@link Simulator}, possibly null
	 * @param simulationResult    the {@link SimulationResult}
	 * @param updateActiveQuarter should the currently active quarter also get
	 *                            updated
	 */
	private void applySimulationResult(Simulator simulator, SimulationResult simulationResult,
			boolean updateActiveQuarter) {
		final Clock clock;
		if (simulator != null) {
			// Debug Log best Schedule
			logSimulationResult(simulator, simulationResult);
			clock = simulator.gsc.clock();
		} else {
			clock = Clock.systemDefaultZone();
		}

		final var thisQuarter = roundDownToQuarter(ZonedDateTime.now(clock));
		final var nextQuarter = thisQuarter.plusMinutes(15);

		// Store result
		this.simulationResult = simulationResult;

		// Send Schedule to Controllers
		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule //
					.tailMap(updateActiveQuarter //
							? thisQuarter // update also current quarter
							: nextQuarter)); // otherwise -> start with next quarter
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
