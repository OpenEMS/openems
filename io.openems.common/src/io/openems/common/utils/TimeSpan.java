package io.openems.common.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class TimeSpan {
	/**
	 * Creates a new timespan that is between the passed start and end date.
	 *
	 * @param startInclusive Start date (inclusive)
	 * @param endExclusive   End date (exclusive)
	 * @return New timespan instance
	 */
	public static TimeSpan between(Instant startInclusive, Instant endExclusive) {
		return new TimeSpan(startInclusive, endExclusive);
	}

	private final Instant startInclusive;
	private final Instant endExclusive;

	protected TimeSpan(Instant startInclusive, Instant endExclusive) {
		this.startInclusive = startInclusive;
		this.endExclusive = endExclusive;
	}

	public Instant getStartInclusive() {
		return this.startInclusive;
	}

	public Instant getEndExclusive() {
		return this.endExclusive;
	}

	public Duration getDuration() {
		return Duration.between(this.startInclusive, this.endExclusive);
	}

	/**
	 * Checks if this timespan overlaps with another one.
	 *
	 * @param other Other timespan
	 * @return true if there is a overlap
	 */
	public boolean overlapsWith(TimeSpan other) {
		// https://www.baeldung.com/java-check-two-date-ranges-overlap#1-using-calendar
		return this.endExclusive.isAfter(other.getStartInclusive())
				&& this.startInclusive.isBefore(other.getEndExclusive());
	}

	/**
	 * Checks if this timespan is contained in another timespan.
	 *
	 * @param other Timespan to check against
	 * @return true if it can be contained
	 */
	public boolean isContainedIn(TimeSpan other) {
		return DateUtils.isAfterOrEquals(this.startInclusive, other.getStartInclusive())
				&& this.endExclusive.isBefore(other.getEndExclusive());
	}

	/**
	 * Returns the TimeSpan that contains the overlapping time between two
	 * timespans. Returns Optional.empty() if there is no overlap.
	 *
	 * @param other Timespan to check against
	 * @return TimeSpan or empty
	 */
	public Optional<TimeSpan> getOverlappingTime(TimeSpan other) {
		var start = DateUtils.max(this.startInclusive, other.getStartInclusive());
		var end = DateUtils.min(this.endExclusive, other.getEndExclusive());

		if (end.isBefore(start)) {
			return Optional.empty();
		}
		return Optional.of(TimeSpan.between(start, end));
	}

	/**
	 * Change this timespan to have the given start date. If the start date can not
	 * be used (because it's after end date), it returns Optional.empty()
	 *
	 * @param start Start date that should be set
	 * @return TimeSpan or Optional.empty()
	 */
	public Optional<TimeSpan> narrowDownToStartDate(Instant start) {
		if (this.startInclusive.isBefore(start)) {
			return Optional.of(this);
		} else if (this.endExclusive.isBefore(start)) {
			return Optional.empty();
		} else {
			return Optional.of(TimeSpan.between(start, this.endExclusive));
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof TimeSpan other)) {
			return false;
		}

		return this.startInclusive.equals(other.getStartInclusive())
				&& this.endExclusive.equals(other.getEndExclusive());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.startInclusive, this.endExclusive);
	}
}
