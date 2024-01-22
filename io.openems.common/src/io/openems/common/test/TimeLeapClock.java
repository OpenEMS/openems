package io.openems.common.test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalUnit;

public class TimeLeapClock extends Clock {

	private final ZoneId zone;

	private volatile Instant instant;

	public TimeLeapClock(Instant start, ZoneId zone) {
		this.instant = start;
		this.zone = zone;
	}

	public TimeLeapClock(ZoneId zone) {
		this(Instant.now(), zone);
	}

	public TimeLeapClock(Instant start) {
		this(start, ZoneOffset.UTC);
	}

	public TimeLeapClock() {
		this(Instant.now(), ZoneOffset.UTC);
	}

	@Override
	public Clock withZone(ZoneId zone) {
		if (zone.equals(this.zone)) { // intentional NPE
			return this;
		}
		return new TimeLeapClock(this.instant, zone);
	}

	@Override
	public ZoneId getZone() {
		return this.zone;
	}

	/**
	 * Add a time leap to the {@link TimeLeapClock}.
	 * 
	 * @param amountToAdd the amount to add
	 * @param unit        the {@link TemporalUnit}
	 */
	public void leap(long amountToAdd, TemporalUnit unit) {
		this.instant = this.instant.plus(amountToAdd, unit);
	}

	@Override
	public long millis() {
		return this.instant.toEpochMilli();
	}

	@Override
	public Instant instant() {
		return this.instant;
	}

	@Override
	public boolean equals(Object obj) {
		return obj == this //
				|| obj instanceof Clock other //
						&& this.instant.equals(other.instant()) //
						&& this.zone.equals(other.getZone());
	}

	@Override
	public int hashCode() {
		return this.instant.hashCode() ^ this.zone.hashCode();
	}

	@Override
	public String toString() {
		return "TimeLeapClock[" + this.instant + ", " + this.zone + "]";
	}

	/**
	 * Get current DateTime as {@link ZonedDateTime}.
	 *
	 * @return current date and time.
	 */
	public ZonedDateTime now() {
		return ZonedDateTime.ofInstant(this.instant, this.zone);
	}
}
