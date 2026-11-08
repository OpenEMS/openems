package io.openems.edge.timeofusetariff.octopus.dynamic.gridbuy;

/**
 * Octopus Energy markets and their Kraken GraphQL API endpoints.
 *
 * <p>
 * Only countries flagged as supported have been tested with this component.
 * The endpoints of the other countries are prepared, but the component does not
 * send any request for them yet.
 *
 * <p>
 * Prices are converted from ct/kWh assuming a currency with 100 subunits per
 * unit (e.g. EUR). Countries with other price units (e.g. pence or yen) need
 * their own unit handling before they can be enabled.
 */
public enum Country {
	GERMANY("https://api.oeg-kraken.energy/v1/graphql/", true), //
	UNITED_KINGDOM("https://api.octopus.energy/v1/graphql/", false), //
	FRANCE("https://api.oefr-kraken.energy/v1/graphql/", false), //
	ITALY("https://api.oeit-kraken.energy/v1/graphql/", false), //
	SPAIN("https://api.oees-kraken.energy/v1/graphql/", false), //
	JAPAN("https://api.oejp-kraken.energy/v1/graphql/", false), //
	USA("https://api.oeus-kraken.energy/v1/graphql/", false), //
	;

	private final String apiUrl;
	private final boolean supported;

	private Country(String apiUrl, boolean supported) {
		this.apiUrl = apiUrl;
		this.supported = supported;
	}

	/**
	 * Gets the URL of the Kraken GraphQL API of this country.
	 *
	 * @return the API URL
	 */
	public String getApiUrl() {
		return this.apiUrl;
	}

	/**
	 * Whether this country has been tested and is enabled.
	 *
	 * @return true if supported
	 */
	public boolean isSupported() {
		return this.supported;
	}
}
