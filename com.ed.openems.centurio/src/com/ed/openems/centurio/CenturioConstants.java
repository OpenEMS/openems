package com.ed.openems.centurio;

public class CenturioConstants {

	public final static int POWER_PRECISION = 10;

	public final static int roundToPowerPrecision(float value) {
		return Math.round(value / POWER_PRECISION * POWER_PRECISION);
	}

}
