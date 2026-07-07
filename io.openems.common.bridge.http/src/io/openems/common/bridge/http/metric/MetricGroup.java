package io.openems.common.bridge.http.metric;

import java.time.Duration;

public record MetricGroup(//
		long requestStartetCount, //
		long requestFinishedCount, //
		long requestSuccessCount, //
		long requestFailedCount, //
		Duration wholeDuration, //
		Duration maxDuration //
) {

	public MetricGroup() {
		this(0, 0, 0, 0, Duration.ZERO, Duration.ZERO);
	}

	/**
	 * Creates a new MetricGroup with updated requestStartetCount.
	 *
	 * @param count The new requestStartetCount.
	 * @return A new MetricGroup instance with the updated requestStartetCount.
	 */
	public MetricGroup withRequestStartetCount(long count) {
		return new MetricGroup(count, this.requestFinishedCount, this.requestSuccessCount, this.requestFailedCount,
				this.wholeDuration, this.maxDuration);
	}

	/**
	 * Creates a new MetricGroup with updated requestFinishedCount.
	 *
	 * @param count The new requestFinishedCount.
	 * @return A new MetricGroup instance with the updated requestFinishedCount.
	 */
	public MetricGroup withRequestFinishedCount(long count) {
		return new MetricGroup(this.requestStartetCount, count, this.requestSuccessCount, this.requestFailedCount,
				this.wholeDuration, this.maxDuration);
	}

	/**
	 * Creates a new MetricGroup with updated requestSuccessCount.
	 *
	 * @param count The new requestSuccessCount.
	 * @return A new MetricGroup instance with the updated requestSuccessCount.
	 */
	public MetricGroup withRequestSuccessCount(long count) {
		return new MetricGroup(this.requestStartetCount, this.requestFinishedCount, count, this.requestFailedCount,
				this.wholeDuration, this.maxDuration);
	}

	/**
	 * Creates a new MetricGroup with updated requestFailedCount.
	 *
	 * @param count The new requestFailedCount.
	 * @return A new MetricGroup instance with the updated requestFailedCount.
	 */
	public MetricGroup withRequestFailedCount(long count) {
		return new MetricGroup(this.requestStartetCount, this.requestFinishedCount, this.requestSuccessCount, count,
				this.wholeDuration, this.maxDuration);
	}

	/**
	 * Creates a new MetricGroup with updated wholeDuration.
	 *
	 * @param duration The new wholeDuration.
	 * @return A new MetricGroup instance with the updated wholeDuration.
	 */
	public MetricGroup withWholeDuration(Duration duration) {
		return new MetricGroup(this.requestStartetCount, this.requestFinishedCount, this.requestSuccessCount,
				this.requestFailedCount, duration, this.maxDuration);
	}

	/**
	 * Creates a new MetricGroup with updated maxDuration.
	 *
	 * @param duration The new maxDuration.
	 * @return A new MetricGroup instance with the updated maxDuration.
	 */
	public MetricGroup withMaxDuration(Duration duration) {
		return new MetricGroup(this.requestStartetCount, this.requestFinishedCount, this.requestSuccessCount,
				this.requestFailedCount, this.wholeDuration, duration);
	}

	/**
	 * Calculates the average duration of finished requests.
	 *
	 * @return The average duration as a Duration object. If no requests have been
	 *         finished, returns Duration.ZERO.
	 */
	public Duration averageDuration() {
		if (this.requestFinishedCount == 0) {
			return Duration.ZERO;
		}
		return this.wholeDuration.dividedBy(this.requestFinishedCount);
	}

	@Override
	public String toString() {
		return "MetricGroup{" //
				+ "requestStartetCount=" + this.requestStartetCount //
				+ ", requestFinishedCount=" + this.requestFinishedCount //
				+ ", requestSuccessCount=" + this.requestSuccessCount //
				+ ", requestFailedCount=" + this.requestFailedCount //
				+ ", wholeDuration=" + this.wholeDuration //
				+ ", maxDuration=" + this.maxDuration //
				+ ", averageDuration=" + this.averageDuration() //
				+ '}';
	}

}
