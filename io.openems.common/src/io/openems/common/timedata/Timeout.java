package io.openems.common.timedata;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class Timeout {

	private Instant entryTime = Instant.MIN;
	private Duration timeout;

	private Timeout(Duration duration) {
		this.timeout = duration;
	}

	/**
	 * Get the {@link Timeout} of seconds.
	 * 
	 * @param timeout the amount seconds
	 * @return the {@link Timeout}
	 */
	public static Timeout ofSeconds(int timeout) {
		return new Timeout(Duration.ofSeconds(timeout));
	}

	/**
	 * Get the {@link Timeout} of minutes.
	 * 
	 * @param timeout the amount minutes
	 * @return the {@link Timeout}
	 */
	public static Timeout ofMinutes(int timeout) {
		return new Timeout(Duration.ofMinutes(timeout));
	}

	/**
	 * Sets the entry time.
	 * 
	 * @param clock the {@link Clock}
	 */
	public void start(Clock clock) {
		this.entryTime = Instant.now(clock);
	}

	/**
	 * Checks the whether time elapsed.
	 * 
	 * @param clock the {@link Clock}
	 * @return true if time is elapsed
	 */
	public boolean elapsed(Clock clock) {
		return Instant.now(clock).isAfter(this.entryTime.plus(this.timeout));
	}

}
