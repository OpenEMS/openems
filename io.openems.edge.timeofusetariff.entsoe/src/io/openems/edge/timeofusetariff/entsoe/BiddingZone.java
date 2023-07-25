package io.openems.edge.timeofusetariff.entsoe;

import io.openems.common.types.OptionsEnum;

public enum BiddingZone implements OptionsEnum {
	UNDEFINED(-1, "Undefined", "Undefined"), //
	GERMANY(0, "10Y1001A1001A82H", "BZN|DE-LU"), //
	AUSTRIA(1, "10YAT-APG------L", "BZN|AT"), //
	SWEDEN_ZONE_1(2, "10Y1001A1001A44P", "BZN|SE1"), //
	SWEDEN_ZONE_2(3, "10Y1001A1001A45N", "BZN|SE2"), //
	SWEDEN_ZONE_3(4, "10Y1001A1001A46L", "BZN|SE3"), //
	SWEDEN_ZONE_4(5, "10Y1001A1001A47J", "BZN|SE4"), //
	;

	private final int value;
	private final String code;
	private final String zone;

	private BiddingZone(int value, String code, String zone) {
		this.value = value;
		this.code = code;
		this.zone = zone;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}

	@Override
	public int getValue() {
		return this.value;
	}

	@Override
	public String getName() {
		return this.code;
	}

	public String getZone() {
		return this.zone;
	}

}
