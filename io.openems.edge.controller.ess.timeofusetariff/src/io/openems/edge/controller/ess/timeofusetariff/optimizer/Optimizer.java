package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static com.google.common.collect.ImmutableSortedMap.toImmutableSortedMap;
import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.simulate;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSimulatorParams;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.logSchedule;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.updateSchedule;
import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSortedMap;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.controller.ess.timeofusetariff.StateMachine;
import io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.Period;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final Supplier<Context> context;
	private final TreeMap<ZonedDateTime, Period> schedule = new TreeMap<>();

	private Params params = null;

	public Optimizer(Supplier<Context> context) {
		this.context = context;
		initializeRandomRegistryForProduction();

		// Run Optimizer thread in LOW PRIORITY
		this.setPriority(Thread.MIN_PRIORITY);
	}

	@Override
	public void forever() throws InterruptedException {
		this.log.info("# Start next run of Optimizer");

		this.createParams(); // this possibly takes forever

		final var context = this.context.get();
		final var start = Instant.now(context.clock());

		long executionLimitSeconds;

		// Calculate max execution time till next quarter (with buffer)
		executionLimitSeconds = calculateExecutionLimitSeconds(context.clock());

		// Find best Schedule
		var schedule = Simulator.getBestSchedule(this.params, executionLimitSeconds);

		// Re-Simulate and keep best Schedule
		var newSchedule = simulate(this.params, schedule);

		// Debug Log best Schedule
		logSchedule(this.params, newSchedule);

		// Update Schedule from newly simulated Schedule
		synchronized (this.schedule) {
			updateSchedule(ZonedDateTime.now(context.clock()), this.schedule, newSchedule);
		}

		// Sleep remaining time
		if (!(context.clock() instanceof TimeLeapClock)) {
			var remainingExecutionLimit = Duration
					.between(Instant.now(context.clock()), start.plusSeconds(executionLimitSeconds)).getSeconds();
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
					this.params = createSimulatorParams(this.context.get(), //
							this.schedule.entrySet().stream() //
									.collect(toImmutableSortedMap(//
											ZonedDateTime::compareTo, //
											Entry::getKey, e -> e.getValue().state())));
					return;
				}

			} catch (InvalidValueException e) {
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
	 * Gets the current {@link StateMachine} or null.
	 * 
	 * @return {@link StateMachine} or null
	 */
	public StateMachine getCurrentStateMachine() {
		synchronized (this.schedule) {
			var period = this.schedule.get(roundDownToQuarter(ZonedDateTime.now()));
			if (period != null) {
				return period.state();
			}
		}
		return null;
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
