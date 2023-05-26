package io.openems.common.timedata;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.OpenemsException;

/**
 * Resolution to hold duration and {@link ChronoUnit}.
 */
public class Resolution {

	private final long value;
	private final ChronoUnit unit;

	public Resolution(long value, String unit) throws OpenemsException {
		this(value, ChronoUnit.valueOf(unit.toUpperCase()));
	}

	public Resolution(long value, ChronoUnit unit) {
		this.value = value;
		this.unit = unit;
	}

	public long getValue() {
		return this.value;
	}

	public ChronoUnit getUnit() {
		return this.unit;
	}

	/**
	 * Revert InfluxDB offset happens if windows resolution is days or months.
	 *
	 * @param date to remove InfluxDB offset
	 * @return Date without offset
	 */
	public ZonedDateTime revertInfluxDbOffset(ZonedDateTime date) {
		return switch (this.unit) {
		case DAYS, MONTHS ->
			 date.minus(this.value, this.unit);		 
		case MINUTES, HOURS -> date;
		default -> date;
			
		};
	}

	/**
	 * Get given duration in seconds [s].
	 *
	 * @return Seconds
	 */
	public Long toSeconds() {
		return Duration.of(this.value, this.unit).toSeconds();
	}

}
