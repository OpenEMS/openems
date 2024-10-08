package io.openems.edge.energy.optimizer;

import static io.jenetics.engine.Limits.byExecutionTime;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.lang.Thread.sleep;
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

	private GlobalSimulationsContext gsc = null;
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
				this.interruptFlag.set(false);
				final var cache = new GenotypeCache();

				if (this.simulationResult == SimulationResult.EMPTY) {
					// No Schedule available yet. Start with a default Schedule with all States
					// set to default.
					this.traceLog(() -> "No existing schedule available -> apply default");
					this.applyBestQuickSchedule(cache);
				}

				var sim = this.runSimulation(cache).get();
				if (sim == null /* simulation gave no result */ || this.interruptFlag.get() /* was interrupted */) {
					this.traceLog(() -> "Simulation gave no result or was interrupted!");
					this.simulationsPerQuarterChannel.setNextValue(null);
					this.applyBestQuickSchedule(cache);
					continue;
				}

				// Calculate metric
				this.simulationsPerQuarterChannel.setNextValue(sim.context.simulationCounter().get());

				// Apply simulation result to EnergyScheduleHandlers
				this.applySimulationResult(cache, sim.context, sim.result, false);
			}
		} catch (InterruptedException | ExecutionException e) {
			// ignore
		} catch (Exception e) {
			this.log.error("OPTIMIZER execution failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private CompletableFuture<Result> runSimulation(GenotypeCache cache) {
		this.traceLog(() -> "Run next Simulation");
		return CompletableFuture.supplyAsync(() -> {
			this.traceLog(() -> "Executing async Simulation");

			final var gsc = this.initializeGlobalSimulationsContext();
			if (gsc == null) {
				return null;
			}

			final var executionLimit = byExecutionTime(ofSeconds(calculateExecutionLimitSeconds()));

			// Find best Schedule
			var bestSchedule = Simulator.getBestSchedule(cache, gsc, this.simulationResult, null, //
					stream -> stream //
							// Stop on interruptFlag
							.limit(ignore -> !this.interruptFlag.get()) //
							// Stop till next quarter
							.limit(executionLimit));

			return new Result(gsc, bestSchedule, cache.getCacheHits());
		});
	}

	private static record Result(GlobalSimulationsContext context, SimulationResult result, int cacheHits) {
	}

	private synchronized GlobalSimulationsContext initializeGlobalSimulationsContext() {
		this.updateGlobalSimulationsContext(); // this possibly takes forever
		var gsc = this.gsc;
		if (gsc == null) {
			return null;
		}

		// Initialize EnergyScheduleHandlers
		gsc.initializeEnergyScheduleHandlers();
		return gsc;
	}

	/**
	 * Create and apply the best quickly available Schedule.
	 * 
	 * @param cache the {@link GenotypeCache}
	 */
	protected synchronized void applyBestQuickSchedule(GenotypeCache cache) {
		this.interruptFlag.set(false);
		final var gsc = this.initializeGlobalSimulationsContext();
		if (gsc == null) {
			this.applyEmptySimulationResult();
			return;
		}

		// Find Genotype with lowest cost
		var bestGt = QuickSchedules.findBestQuickSchedule(cache, gsc, this.simulationResult);
		if (bestGt == null) {
			this.applyEmptySimulationResult();
			return;
		}
		var simulationResult = SimulationResult.fromQuarters(gsc, bestGt);

		this.traceLog(() -> "Applying best quick Schedule");
		this.applySimulationResult(cache, gsc, simulationResult, true);
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public synchronized void deactivate() {
		this.interruptFlag.set(true);
	}

	private void applyEmptySimulationResult() {
		this.traceLog(() -> "Applying empty Schedule");
		this.applySimulationResult(null, null, SimulationResult.EMPTY, true);
	}

	/**
	 * Applies the Schedule to all {@link EnergyScheduleHandler}s and stores the
	 * {@link SimulationResult} in `this.simulationResult`.
	 *
	 * @param cache               the {@link GenotypeCache}
	 * @param gsc                 the {@link GlobalSimulationsContext}, possibly
	 *                            null
	 * @param simulationResult    the {@link SimulationResult}
	 * @param updateActiveQuarter should the currently active quarter also get
	 *                            updated
	 */
	private void applySimulationResult(GenotypeCache cache, GlobalSimulationsContext gsc,
			SimulationResult simulationResult, boolean updateActiveQuarter) {
		final Clock clock;
		if (gsc != null) {
			// Debug Log best Schedule
			logSimulationResult(cache, gsc, simulationResult);
			clock = gsc.clock();
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

	/**
	 * Updates the {@link GlobalSimulationsContext}.
	 * 
	 * <p>
	 * This will possibly run forever and set the global `gsc` variable. In case of
	 * error `gsc` is set to `null`.
	 */
	private void updateGlobalSimulationsContext() {
		while (!this.interruptFlag.get()) {
			try {
				this.gsc = this.gscSupplier.get();
				return;

			} catch (OpenemsException | IllegalArgumentException e) {
				this.traceLog(() -> "Stuck trying to get GlobalSimulationsContext. " + e.getMessage());
				this.gsc = null;
				this.applyEmptySimulationResult();
				try {
					sleep(10_000);
				} catch (InterruptedException e1) {
					this.traceLog(() -> "Unable to create global simulations context: " + e1.getMessage());
					this.gsc = null;
					return;
				}
			}
		}
		this.traceLog(() -> "Unable to create global simulations context -> abort");
		this.gsc = null;
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
		var gsc = this.gsc;
		if (gsc != null) {
			b.append("|SimulationCounter:" + gsc.simulationCounter().get());
		}
		b.append("|PerQuarter:" + this.simulationsPerQuarterChannel.value());
		return b.toString();
	}
}
