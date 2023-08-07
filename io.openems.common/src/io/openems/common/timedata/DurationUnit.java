package io.openems.common.timedata;

import java.time.Duration;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;

public final class DurationUnit implements TemporalUnit {

	private static final int SECONDS_PER_DAY = 86400;
	private static final long NANOS_PER_SECOND = 1000_000_000L;
	private static final long NANOS_PER_DAY = NANOS_PER_SECOND * SECONDS_PER_DAY;

	private final Duration duration;

	/**
	 * Get the {@link DurationUnit} for {@link Duration}.
	 * 
	 * @param duration the {@link Duration}
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit of(Duration duration) {
		return new DurationUnit(duration);
	}

	/**
	 * Get the {@link DurationUnit} of days.
	 * 
	 * @param days the amount days
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofDays(long days) {
		return new DurationUnit(Duration.ofDays(days));
	}

	/**
	 * Get the {@link DurationUnit} of hours.
	 * 
	 * @param hours the amount hours
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofHours(long hours) {
		return new DurationUnit(Duration.ofHours(hours));
	}

	/**
	 * Get the {@link DurationUnit} of minutes.
	 * 
	 * @param minutes the amount minutes
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofMinutes(long minutes) {
		return new DurationUnit(Duration.ofMinutes(minutes));
	}

	/**
	 * Get the {@link DurationUnit} of seconds.
	 * 
	 * @param seconds the amount seconds
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofSeconds(long seconds) {
		return new DurationUnit(Duration.ofSeconds(seconds));
	}

	/**
	 * Get the {@link DurationUnit} of millis.
	 * 
	 * @param millis the amount millis
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofMillis(long millis) {
		return new DurationUnit(Duration.ofMillis(millis));
	}

	/**
	 * Get the {@link DurationUnit} of nanos.
	 * 
	 * @param nanos the amount nanos
	 * @return the {@link DurationUnit}
	 */
	public static DurationUnit ofNanos(long nanos) {
		return new DurationUnit(Duration.ofNanos(nanos));
	}

	private DurationUnit(Duration duration) {
		if (duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException("Duration may not be zero or negative");
		}
		this.duration = duration;
	}

	@Override
	public Duration getDuration() {
		return this.duration;
	}

	@Override
	public boolean isDurationEstimated() {
		return this.duration.getSeconds() >= SECONDS_PER_DAY;
	}

	@Override
	public boolean isDateBased() {
		return this.duration.getNano() == 0 && this.duration.getSeconds() % SECONDS_PER_DAY == 0;
	}

	@Override
	public boolean isTimeBased() {
		return this.duration.getSeconds() < SECONDS_PER_DAY && NANOS_PER_DAY % this.duration.toNanos() == 0;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <R extends Temporal> R addTo(R temporal, long amount) {
		return (R) this.duration.multipliedBy(amount).addTo(temporal);
	}

	@Override
	public long between(Temporal temporal1Inclusive, Temporal temporal2Exclusive) {
		return Duration.between(temporal1Inclusive, temporal2Exclusive).dividedBy(this.duration);
	}

	@Override
	public String toString() {
		return this.duration.toString();
	}

}