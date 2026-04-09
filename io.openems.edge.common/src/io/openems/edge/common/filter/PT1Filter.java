package io.openems.edge.common.filter;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PT1Filter extends Filter {

	public static final int DEFAULT_TIME_CONSTANT = 800;

	/**
	 * Time constant of the PT1 filter in milliseconds [ms].
	 */
	public final int timeConstant;

	private final Logger log = LoggerFactory.getLogger(PT1Filter.class);
	private final Clock clock;

	private Instant lastApplyTimestamp = null;
	private double lastOutput;

	/**
	 * Creates a PT1 filter with a {@link Clock} for testing purposes.
	 * 
	 * @param clock        the {@link Clock}
	 * @param timeConstant the time constant of the PT1 filter
	 */
	public PT1Filter(Clock clock, int timeConstant) {
		this.clock = clock;
		this.timeConstant = timeConstant;
	}

	/**
	 * Creates a PT1 filter.
	 * 
	 * @param timeConstant time constant of the PT1 filter in milliseconds [ms]; '0'
	 *                     disables the filter
	 */
	public PT1Filter(int timeConstant) {
		this(Clock.systemDefaultZone(), timeConstant);
	}

	@Override
	public void reset() {
		this.lastApplyTimestamp = null;
	}

	/**
	 * Apply the PT1 filter.
	 * 
	 * @param value the input value
	 * @return the filtered value
	 */
	public int applyPT1Filter(double value) {
		final var now = Instant.now(this.clock);

		final double output;
		if (this.lastApplyTimestamp == null // handle first call
				|| this.timeConstant == 0) { // handle disabled filter
			this.lastApplyTimestamp = now;
			output = value;

		} else {
			final var cycleTime = Duration.between(this.lastApplyTimestamp, now).toMillis();
			if (cycleTime <= 50) {
				// handle very short cycle time (e.g. due to multiple calls in the same cycle)
				// to avoid -> do not update lastApplyTimestamp; return previous output
				this.log.info("PT1Filter: filter is applied more frequently than expected " //
						+ "[cycle time=" + cycleTime + "ms]");
				output = this.lastOutput; // do not apply filter if cycle time is invalid

			} else {
				// Apply filter
				this.lastApplyTimestamp = now;
				final var factor = this.timeConstant / (double) cycleTime;
				output = (value + factor * this.lastOutput) / (1F + factor);
			}
		}

		this.lastOutput = output;
		return (int) Math.round(output);
	}
}