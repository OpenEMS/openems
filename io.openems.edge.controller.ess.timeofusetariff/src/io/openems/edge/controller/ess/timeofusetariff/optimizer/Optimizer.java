package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.calculateExecutionLimitSeconds;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSimulatorParams;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.random.RandomGeneratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.jenetics.util.RandomRegistry;
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

		/* Initialize 'Random' */
		// Default RandomGenerator "L64X256MixRandom" might not be available. Choose
		// best available.
		System.setProperty("io.jenetics.util.defaultRandomGenerator", "Random");
		var rgf = RandomGeneratorFactory.all() //
				.filter(RandomGeneratorFactory::isStatistical) //
				.sorted((f, g) -> Integer.compare(g.stateBits(), f.stateBits())).findFirst()
				.orElse(RandomGeneratorFactory.of("Random"));
		RandomRegistry.random(rgf.create());
	}

	@Override
	public void forever() throws InterruptedException {
		this.log.info("# Start next run of Optimizer");

		final var context = this.context.get();
		final var start = Instant.now(context.clock());

		long executionLimitSeconds;
		try {
			this.params = this.createParams();

			// Calculate max execution time till next quarter (with buffer)
			executionLimitSeconds = calculateExecutionLimitSeconds(context.clock());

			// Find best Schedule
			var schedule = Simulator.getBestSchedule(this.params, executionLimitSeconds);

			// Re-Simulate and keep best Schedule
			var periods = new TreeMap<ZonedDateTime, Period>();
			calculateCost(this.params, schedule, period -> periods.put(period.time(), period));
			synchronized (this.periods) {
				var thisQuarter = roundZonedDateTimeDownToMinutes(ZonedDateTime.now(context.clock()), 15);
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
			this.logBestSchedule(periods);

		} catch (InvalidValueException | InterruptedException e) {
			this.log.error("Error while running Optimizer: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
			synchronized (this.periods) {
				this.periods.clear();
			}

			// Recalculate execution/sleep time
			executionLimitSeconds = calculateExecutionLimitSeconds(context.clock());
		}

		// Sleep remaining time
		if (!(context.clock() instanceof TimeLeapClock)) {
			var remainingExecutionLimit = Duration
					.between(Instant.now(context.clock()), start.plusSeconds(executionLimitSeconds)).getSeconds();
			if (remainingExecutionLimit > 0) {
				this.log.info("Sleep [" + (remainingExecutionLimit / 1000) + "s] till next run of Optimizer");
				Thread.sleep(remainingExecutionLimit);
			}
		}
	}

	// On first run, we allow multiple retries, till all data is available (e.g. ESS
	// Capacity)
	private int allowRetries = 10;

	private Params createParams() throws InvalidValueException, InterruptedException {
		try {
			var result = createSimulatorParams(this.context.get(), this.periods);
			this.allowRetries = 0;
			return result;

		} catch (InvalidValueException e) {
			this.log.info("# Error while getting Params. Retries [" + this.allowRetries + "]. " + e.getMessage());
			e.printStackTrace();
			if (this.allowRetries > 0) {
				this.allowRetries--;
				Thread.sleep(10_000);
				return this.createParams();
			} else {
				throw e;
			}
		}
	}

	/**
	 * Gets the current {@link Params}.
	 * 
	 * @return the {@link Params}
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
			return this.periods.get(roundZonedDateTimeDownToMinutes(ZonedDateTime.now(), 15));
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

	private void logBestSchedule(TreeMap<ZonedDateTime, Period> periods) {
		var b = new StringBuilder("OPTIMIZER ") //
				.append(Period.header()) //
				.append("\n");
		if (periods.values().isEmpty()) {
			b //
					.append("OPTIMIZER ") //
					.append("-> EMPTY\n");
		} else {
			periods.values().stream() //
					.map(Period::toString) //
					.forEach(s -> b //
							.append("OPTIMIZER ") //
							.append(s) //
							.append("\n"));
		}
		System.out.println(b.toString());
	}
}
