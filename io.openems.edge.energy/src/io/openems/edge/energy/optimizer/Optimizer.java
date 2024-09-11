package io.openems.edge.energy.optimizer;

import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.optimizer.Utils.logSimulationResult;
import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.Instant;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.energy.api.simulation.GlobalSimulationsContext;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final BiConsumer<Logger, String> logInfo;
	private final ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier;
	private final Channel<Integer> simulationsPerQuarterChannel;

	private GlobalSimulationsContext globalSimulationsContext = null;
	private SimulationResult simulationResult = SimulationResult.EMPTY;

	public Optimizer(BiConsumer<Logger, String> logInfo,
			ThrowingSupplier<GlobalSimulationsContext, OpenemsException> gscSupplier, //
			Channel<Integer> simulationsPerQuarterChannel) {
		this.logInfo = logInfo;
		this.gscSupplier = gscSupplier;
		this.simulationsPerQuarterChannel = simulationsPerQuarterChannel;
		initializeRandomRegistryForProduction();

		// Run Optimizer thread in LOW PRIORITY
		this.setPriority(Thread.MIN_PRIORITY);
	}

	/**
	 * Reset the Optimizer to release references to {@link EnergyScheduleHandler}s.
	 * 
	 * @param name name of parent Component.
	 */
	public void resetOptimizer(String name) {
		this.deactivate();
		this.simulationResult = SimulationResult.EMPTY;
		super.activate(name);
	}

	@Override
	public void forever() throws InterruptedException, OpenemsException {
		this.logInfo("Start next run of Optimizer");

		final var context = this.createGlobalSimulationsContext(); // this possibly takes forever
		this.globalSimulationsContext = context;

		// Initialize EnergyScheduleHandlers
		for (var esh : context.handlers()) {
			esh.onBeforeSimulation(context);
		}

		// Calculate max execution time till next quarter (with buffer)
		final var start = Instant.now(context.clock());
		long executionLimitSeconds;
		executionLimitSeconds = calculateExecutionLimitSeconds(context.clock());

		// Find best Schedule
		var simulationResult = Simulator.getBestSchedule(context, this.simulationResult, executionLimitSeconds);

		// Calculate metric
		this.simulationsPerQuarterChannel.setNextValue(this.globalSimulationsContext.simulationCounter().get());

		// Apply simulation result to EnergyScheduleHandlers
		this.applySimulationResult(simulationResult);

		// Debug Log best Schedule
		logSimulationResult(context, simulationResult);

		// Sleep remaining time
		if (!(context.clock() instanceof TimeLeapClock)) {
			var remainingExecutionLimit = Duration
					.between(Instant.now(context.clock()), start.plusSeconds(executionLimitSeconds)).getSeconds();
			if (remainingExecutionLimit > 0) {
				this.logInfo("Sleep [" + remainingExecutionLimit + "s] till next run of Optimizer");
				sleep(remainingExecutionLimit * 1000);
			}
		}
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
	 * @return the {@link GlobalSimulationsContext}
	 * @throws InterruptedException during sleep
	 */
	private GlobalSimulationsContext createGlobalSimulationsContext() throws InterruptedException {
		while (true) {
			try {
				return this.gscSupplier.get();

			} catch (OpenemsException | IllegalArgumentException e) {
				this.logInfo("Stuck trying to get GlobalSimulationsContext. " + e.getMessage());
				this.globalSimulationsContext = null;
				this.applySimulationResult(SimulationResult.EMPTY);
				sleep(30_000);
			}
		}
	}

	private void logInfo(String message) {
		this.logInfo.accept(this.log, message);
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
