package io.openems.edge.meter.sunspec;

public enum SunSpecMeters {
	SOLAR_EDGE("SolarEdge", 2), ELGRIS_ZERO_EXPORT("Elgris Zero Export", 1),
	ELGRIS_SMART_METER("Elgris Smart Meter", 1);

	public String name;
	public int readFromCommonBlockNo;

	SunSpecMeters(String name, int readFromCommonBlockNo) {
		this.name = name;
		this.readFromCommonBlockNo = readFromCommonBlockNo;
	}

}
