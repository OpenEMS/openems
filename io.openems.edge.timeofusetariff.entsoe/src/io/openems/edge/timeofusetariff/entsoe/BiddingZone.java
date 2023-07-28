package io.openems.edge.timeofusetariff.entsoe;

public enum BiddingZone {
	GERMANY("10Y1001A1001A82H", "BZN|DE-LU"), //
	AUSTRIA("10YAT-APG------L", "BZN|AT"), //
	SWEDEN_ZONE_1("10Y1001A1001A44P", "BZN|SE1"), //
	SWEDEN_ZONE_2("10Y1001A1001A45N", "BZN|SE2"), //
	SWEDEN_ZONE_3("10Y1001A1001A46L", "BZN|SE3"), //
	SWEDEN_ZONE_4("10Y1001A1001A47J", "BZN|SE4"), //
	;

	private final String code;
	private final String zone;

	private BiddingZone(String code, String zone) {
		this.code = code;
		this.zone = zone;
	}

	public String getCode() {
		return this.code;
	}

	public String getZone() {
		return this.zone;
	}

}
