package io.openems.edge.energy.v1.optimizer;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.edge.energy.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.energy.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.energy.v1.optimizer.Simulator.simulate;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.createSimulatorParams;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.logSchedule;
import static io.openems.edge.energy.v1.optimizer.UtilsV1.updateSchedule;
import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.controller.ess.timeofusetariff.v1.EnergyScheduleHandlerV1;
import io.openems.edge.energy.v1.optimizer.Simulator.Period;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class OptimizerV1 extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(OptimizerV1.class);

	private final ThrowingSupplier<GlobalContext, OpenemsException> globalContext;
	private final TreeMap<ZonedDateTime, Period> schedule = new TreeMap<>();

	private Params params = null;

	public OptimizerV1(ThrowingSupplier<GlobalContext, OpenemsException> globalContext) {
		this.globalContext = globalContext;
		initializeRandomRegistryForProduction();

		// Run Optimizer thread in LOW PRIORITY
		this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void forever() throws InterruptedException, OpenemsException {
		this.log.info("# Start next run of Optimizer");

		this.createParams(); // this possibly takes forever

		final var globalContext = this.globalContext.get();
		final var start = Instant.now(globalContext.clock());

		long executionLimitSeconds;

		// Calculate max execution time till next quarter (with buffer)
		executionLimitSeconds = calculateExecutionLimitSeconds(globalContext.clock());

		// Find best Schedule
		var schedule = Simulator.getBestSchedule(this.params, executionLimitSeconds);

		// Re-Simulate and keep best Schedule
		var newSchedule = simulate(this.params, schedule);

		// Debug Log best Schedule
		logSchedule(this.params, newSchedule);

		// Update Schedule from newly simulated Schedule
		synchronized (this.schedule) {
			updateSchedule(ZonedDateTime.now(globalContext.clock()), this.schedule, newSchedule);
		}

		// Send Schedule to Controller
		globalContext.energyScheduleHandler().setSchedule(this.schedule.entrySet().stream()//
				.collect(toImmutableSortedMap(//
						ZonedDateTime::compareTo, //
						Entry::getKey, //
						e -> new EnergyScheduleHandlerV1.Period<>(e.getValue().state(),
								e.getValue().op().essChargeInChargeGrid()))));

		// Sleep remaining time
		if (!(globalContext.clock() instanceof TimeLeapClock)) {
			var remainingExecutionLimit = Duration
					.between(Instant.now(globalContext.clock()), start.plusSeconds(executionLimitSeconds)).getSeconds();
			if (remainingExecutionLimit > 0) {
				this.log.info("Sleep [" + remainingExecutionLimit + "s] till next run of Optimizer");
				sleep(remainingExecutionLimit * 1000);
			}
		}
	}

	/**
	 * Try forever till all data is available (e.g. ESS Capacity)
	 * 
	 * @throws InterruptedException during sleep
	 */
	private void createParams() throws InterruptedException {
		while (true) {
			try {
				synchronized (this.schedule) {
					this.params = createSimulatorParams(this.globalContext.get(), //
							this.schedule.entrySet().stream() //
									.collect(toImmutableSortedMap(//
											ZonedDateTime::compareTo, //
											Entry::getKey, e -> e.getValue().state())));
					return;
				}

			} catch (OpenemsException e) {
				this.log.info("# Stuck trying to get Params. " + e.getMessage());
				this.params = null;
				synchronized (this.schedule) {
					this.schedule.clear();
				}
				sleep(30_000);
			}
		}
	}

	/**
	 * Gets the current {@link Params} or null.
	 * 
	 * @return the {@link Params} or null
	 */
	public Params getParams() {
		return this.params;
	}

	/**
	 * Gets a copy of the Schedule.
	 * 
	 * @return {@link ImmutableSortedMap}
	 */
	public ImmutableSortedMap<ZonedDateTime, Period> getSchedule() {
		synchronized (this.schedule) {
			return ImmutableSortedMap.copyOf(this.schedule);
		}
	}
}
