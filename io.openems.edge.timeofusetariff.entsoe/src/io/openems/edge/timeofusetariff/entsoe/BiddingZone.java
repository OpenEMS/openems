package io.openems.edge.timeofusetariff.entsoe;

/**
 * https://transparency.entsoe.eu/content/static_content/Static%20content/web%20api/Guide.html#_areas.
 */
public enum BiddingZone {
	/**
	 * BZN|DE-LU.
	 */
	GERMANY("10Y1001A1001A82H"), //
	/**
	 * BZN|AT.
	 */
	AUSTRIA("10YAT-APG------L"), //
	/**
	 * BZN|SE1.
	 */
	SWEDEN_SE1("10Y1001A1001A44P"), //
	/**
	 * BZN|SE2.
	 */
	SWEDEN_SE2("10Y1001A1001A45N"), //
	/**
	 * BZN|SE3.
	 */
	SWEDEN_SE3("10Y1001A1001A46L"), //
	/**
	 * BZN|SE4.
	 */
	SWEDEN_SE4("10Y1001A1001A47J"), //
	;

	private final String code;

	private BiddingZone(String code) {
		this.code = code;
	}

	public String getCode() {
		return this.code;
	}
}
