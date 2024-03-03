package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundDownToQuarter;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSimulatorParams;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.initializeRandomRegistryForProduction;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.logSchedule;
import static java.lang.Thread.sleep;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.common.test.TimeLeapClock;
import io.openems.common.worker.AbstractImmediateWorker;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final Supplier<Context> context;
	private final TreeMap<ZonedDateTime, Period> periods = new TreeMap<>();

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
		var periods = new TreeMap<ZonedDateTime, Period>();
		calculateCost(this.params, schedule, period -> periods.put(period.time(), period));
		synchronized (this.periods) {
			var thisQuarter = roundDownToQuarter(ZonedDateTime.now(context.clock()));
			// Do not overwrite the current quarter
			if (this.periods.containsKey(thisQuarter)) {
				periods.remove(thisQuarter);
			}
			// Add new entries
			this.periods.putAll(periods);
			// Remove outdated entries
			this.periods.headMap(thisQuarter).clear();
		}

		// Debug Log best Schedule
		logSchedule(this.params, periods);

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
				this.params = createSimulatorParams(this.context.get(), this.periods);
				return;

			} catch (InvalidValueException e) {
				this.log.info("# Stuck trying to get Params. " + e.getMessage());
				this.params = null;
				synchronized (this.periods) {
					this.periods.clear();
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
	 * Gets the current {@link Period} or null.
	 * 
	 * @return Period or null
	 */
	public Period getCurrentPeriod() {
		synchronized (this.periods) {
			return this.periods.get(roundDownToQuarter(ZonedDateTime.now()));
		}
	}

	/**
	 * Gets a copy of the {@link Period}s list.
	 * 
	 * @return {@link ImmutableList} of {@link Period}s
	 */
	protected ImmutableList<Period> getPeriods() {
		final ImmutableList<Period> result;
		synchronized (this.periods) {
			result = ImmutableList.copyOf(this.periods.values());
		}
		return result;
	}
}
