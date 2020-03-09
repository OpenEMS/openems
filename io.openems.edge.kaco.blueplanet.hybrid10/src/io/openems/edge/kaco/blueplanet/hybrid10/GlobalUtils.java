package io.openems.edge.kaco.blueplanet.hybrid10;

public class GlobalUtils {

	public final static int POWER_PRECISION = 1;

	public final static int roundToPowerPrecision(float value) {
		return Math.round(value / POWER_PRECISION * POWER_PRECISION);
	}

}
