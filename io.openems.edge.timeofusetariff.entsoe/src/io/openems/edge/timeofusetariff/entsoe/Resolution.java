package io.openems.edge.timeofusetariff.entsoe;

import java.time.Duration;

public enum Resolution {

	/**
	 * Prices every Hour.
	 */
	HOURLY("PT60M"), //
	/**
	 * Prices every Quarter.
	 */
	QUARTERLY("PT15M");

	public final String resolutionCode;
	public final Duration duration;

	private Resolution(String resolutionCode) {
		this.resolutionCode = resolutionCode;
		this.duration = Duration.parse(resolutionCode);
	}

}
