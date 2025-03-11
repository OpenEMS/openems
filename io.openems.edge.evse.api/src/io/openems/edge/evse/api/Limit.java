package io.openems.edge.evse.api;

import static io.openems.edge.common.type.TypeUtils.fitWithin;
import static java.lang.Math.round;

public record Limit(SingleThreePhase phase, int minCurrent, int maxCurrent) {

	/**
	 * Gets the Min-Power in [W].
	 * 
	 * @return min power
	 */
	public int getMinPower() {
		return round(this.minCurrent / 1000f * this.phase.count * 230f);
	}

	/**
	 * Gets the Max-Power in [W].
	 * 
	 * @return max power
	 */
	public int getMaxPower() {
		return round(this.maxCurrent / 1000f * this.phase.count * 230f);
	}

	/**
	 * Calculates the Current from Power, considering {@link SingleThreePhase},
	 * minCurrent and maxCurrent.
	 * 
	 * @param power the power
	 * @return the current
	 */
	public int calculateCurrent(int power) {
		var current = round((power * 1000) / this.phase.count / 230f);
		return fitWithin(this.minCurrent, this.maxCurrent, current);
	}
}
