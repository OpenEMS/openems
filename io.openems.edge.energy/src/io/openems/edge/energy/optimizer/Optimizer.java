package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.lang.Thread.sleep;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jenetics.engine.Limits;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.utils.FunctionUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.LogVerbosity;
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

	private GlobalSimulationsContext globalSimulationsContext = null;
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
	 * Reset and re-run the {@link Optimizer}.
	 */
	public void reset() {
		this.interruptFlag.set(true);
	}

	@Override
	public void run() {
		try {
			while (true) {
				this.interruptFlag.set(false);
				var sim = this.runSimulation().get();
				if (sim == null) {
					this.traceLog(() -> "Simulation gave no result!");
					this.simulationsPerQuarterChannel.setNextValue(null);
					this.applySimulationResult(SimulationResult.EMPTY);
					continue;
				}

				// Calculate metric
				this.simulationsPerQuarterChannel.setNextValue(sim.context.simulationCounter().get());

				// Apply simulation result to EnergyScheduleHandlers
				this.applySimulationResult(sim.result);

				// Debug Log best Schedule
				logSimulationResult(sim.context, sim.result);
			}
		} catch (InterruptedException | ExecutionException e) {
			// ignore
		} catch (Exception e) {
			this.log.error("OPTIMIZER execution failed: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private CompletableFuture<Result> runSimulation() {
		this.traceLog(() -> "Run next Simulation");
		return CompletableFuture.supplyAsync(() -> {
			this.traceLog(() -> "Executing async Simulation");

			final var context = this.createGlobalSimulationsContext(); // this possibly takes forever
			if (context == null) {
				this.traceLog(() -> "Unable to create global simulations context -> abort");
				return null;
			}
			this.globalSimulationsContext = context; // for debug log of simulation counter

			// Initialize EnergyScheduleHandlers
			for (var esh : context.handlers()) {
				esh.onBeforeSimulation(context);
			}

			if (this.simulationResult == SimulationResult.EMPTY) {
				this.traceLog(() -> "No existing schedule available");
			}

			final var executionLimit = Limits.byExecutionTime(Duration.ofSeconds(calculateExecutionLimitSeconds()));

			// Find best Schedule
			var bestSchedule = Simulator.getBestSchedule(context, this.simulationResult, null, //
					stream -> stream //
							// Stop on interruptFlag
							.limit(ignore -> !this.interruptFlag.get()) //
							// Stop till next quarter
							.limit(executionLimit));

			return new Result(context, bestSchedule);
		});
	}

	private static record Result(GlobalSimulationsContext context, SimulationResult result) {
	}

	/**
	 * Deactivate the {@link Optimizer}.
	 */
	public synchronized void deactivate() {
		this.interruptFlag.set(true);
	}

	private void applySimulationResult(SimulationResult simulationResult) {
		// Store result
		this.simulationResult = simulationResult;

		// Send Schedule to Controllers
		simulationResult.schedules().forEach((esh, schedule) -> {
			esh.applySchedule(schedule);
		});
	}

	/**
	 * Try forever till all data is available.
	 * 
	 * @return the {@link GlobalSimulationsContext}; or null on
	 *         {@link InterruptedException} or if `interruptFlat` was set
	 */
	private GlobalSimulationsContext createGlobalSimulationsContext() {
		while (!this.interruptFlag.get()) {
			try {
				return this.gscSupplier.get();

			} catch (OpenemsException | IllegalArgumentException e) {
				this.traceLog(() -> "Stuck trying to get GlobalSimulationsContext. " + e.getMessage());
				this.globalSimulationsContext = null;
				this.applySimulationResult(SimulationResult.EMPTY);
				try {
					sleep(10_000);
				} catch (InterruptedException e1) {
					return null;
				}
			}
		}
		return null;
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
		var gsc = this.globalSimulationsContext;
		if (gsc != null) {
			b.append("|SimulationCounter:" + gsc.simulationCounter().get());
		}
		b.append("|PerQuarter:" + this.simulationsPerQuarterChannel.value());
		return b.toString();
	}
}
