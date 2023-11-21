package io.openems.edge.controller.ess.timeofusetariff.optimizer;

import static io.openems.common.utils.DateUtils.roundZonedDateTimeDownToMinutes;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Simulator.calculateCost;
import static io.openems.edge.controller.ess.timeofusetariff.optimizer.Utils.createSimulatorParams;

import java.time.ZonedDateTime;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.random.RandomGeneratorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

import io.jenetics.util.RandomRegistry;
import io.openems.common.exceptions.InvalidValueException;

/**
 * This task is executed once in the beginning and afterwards every full 15
 * minutes.
 */
public class Optimizer implements Runnable {

	private final Logger log = LoggerFactory.getLogger(Optimizer.class);

	private final Supplier<Context> context;
	private final TreeMap<ZonedDateTime, Period> periods = new TreeMap<>();

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
	public void run() {
		this.log.info("# Start Optimizer");
		var start = System.currentTimeMillis();
		try {
			var params = this.getParams();

			// Find best Schedule
			var schedule = Simulator.getBestSchedule(params);

			// Re-Simulate and keep best Schedule
			var periods = new TreeMap<ZonedDateTime, Period>();
			calculateCost(params, schedule, period -> periods.put(period.time(), period));
			synchronized (this.periods) {
				var thisQuarter = roundZonedDateTimeDownToMinutes(ZonedDateTime.now(), 15);
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
			this.log.info("# Best Schedule");
			var b = new StringBuilder(Period.header());
			b.append("\n");
			periods.values().stream() //
					.map(Period::toString) //
					.forEach(s -> b.append(s).append("\n"));
			System.out.println(b.toString());

		} catch (InvalidValueException | InterruptedException e) {
			this.log.error("Error while running Optimizer: " + e.getClass().getSimpleName() + ": " + e.getMessage());
			e.printStackTrace();
			synchronized (this.periods) {
				this.periods.clear();
			}
		}

		// Measure time
		var solveDuration = System.currentTimeMillis() - start;
		this.context.get().solveDurationChannel().setNextValue(solveDuration);
		this.log.info("# Finished Optimizer after: " + (solveDuration) / 1000 + "s");
	}

	// On first run, we allow multiple retries, till all data is available (e.g. ESS
	// Capacity)
	private int allowRetries = 10;

	private Params getParams() throws InvalidValueException, InterruptedException {
		try {
			var result = createSimulatorParams(this.context.get());
			this.allowRetries = 0;
			return result;

		} catch (InvalidValueException e) {
			this.log.info("# Error while getting Params. Retries [" + this.allowRetries + "]. " + e.getMessage());
			e.printStackTrace();
			if (this.allowRetries > 0) {
				this.allowRetries--;
				Thread.sleep(10_000);
				return this.getParams();
			} else {
				throw e;
			}
		}
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
}
