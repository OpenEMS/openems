package io.openems.edge.timeofusetariff.entsoe;

import java.time.Duration;

public enum Resolution {

	/**
	 * Prices every Hour.
	 */
	HOURLY(Duration.ofMinutes(60)), //
	/**
	 * Prices every Quarter.
	 */
	QUARTERLY(Duration.ofMinutes(15));

	public final Duration duration;

	private Resolution(Duration duration) {
		this.duration = duration;
	}

}
