package io.openems.edge.timeofusetariff.awattar;

/**
 * Represents different energy market zones.
 */
public enum Zone {

	/**
	 * The energy market zone for Germany.
	 */
	GERMANY, //

	/**
	 * The energy market zone for Austria.
	 */
	AUSTRIA;

	/**
	 * Returns the API URL specific to the {@link Zone}.
	 *
	 * @return The {@link Zone} specific API URL.
	 */
	public String toUrl() {
		return switch (this) {
		case GERMANY -> "https://api.awattar.de/v1/marketdata";
		case AUSTRIA -> "https://api.awattar.at/v1/marketdata";
		};
	}
}
