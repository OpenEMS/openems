package io.openems.edge.controller.ortsnetzauslastung;

import io.openems.edge.controller.ortsnetzauslastung.Types.Statistics;

/**
 * Collects minimum, maximum and arithmetic mean of integer samples over one
 * send interval.
 */
class IntervalStatistics {

	private int min;
	private int max;
	private long sum;
	private int count;

	/**
	 * Adds a sample.
	 *
	 * @param value the value; null is ignored
	 */
	public void add(Integer value) {
		if (value == null) {
			return;
		}
		if (this.count == 0) {
			this.min = value;
			this.max = value;
		} else {
			this.min = Math.min(this.min, value);
			this.max = Math.max(this.max, value);
		}
		this.sum += value;
		this.count++;
	}

	/**
	 * Gets the {@link Statistics}, scaled by the given divisor.
	 *
	 * <p>
	 * The mean is rounded to the same resolution as the samples, so that min
	 * &lt;= avg &lt;= max always holds.
	 *
	 * @param divisor e.g. 1000 to convert [mV] to [V]
	 * @return the {@link Statistics} or null if there was no sample
	 */
	public Statistics get(double divisor) {
		if (this.count == 0) {
			return null;
		}
		final var avg = Math.round((double) this.sum / this.count);
		return new Statistics(this.min / divisor, this.max / divisor, avg / divisor);
	}

	/**
	 * Clears all samples.
	 */
	public void reset() {
		this.sum = 0;
		this.count = 0;
	}
}
