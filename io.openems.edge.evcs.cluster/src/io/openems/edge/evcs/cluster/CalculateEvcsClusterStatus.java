package io.openems.edge.evcs.cluster;

import java.util.ArrayList;
import java.util.List;

import io.openems.edge.evcs.api.ChargeState;

/**
 * Helper class to find the Status of multiple EVCS.
 */
public class CalculateEvcsClusterStatus {

	private final List<ChargeState> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 * 
	 * @param status Charge state
	 */
	public void addValue(ChargeState status) {
		this.values.add(status);
	}

	/**
	 * Finds the effective Status for the EvcsCluster.
	 * 
	 * @return current evcs cluster state
	 */
	public EvcsClusterStatus calculate() {
		int increasing = 0;
		int undefined = 0;
		for (ChargeState status : this.values) {
			switch (status) {
			case CHARGING:
			case NOT_CHARGING:
			case WAITING_FOR_AVAILABLE_POWER:
				break;
			case UNDEFINED:
				undefined++;
				break;
			case INCREASING:
				increasing++;
				break;
			case DECREASING:
				return EvcsClusterStatus.DECREASING;
			}
		}
		EvcsClusterStatus result = EvcsClusterStatus.REGULAR;

		if (increasing > 0) {
			result = EvcsClusterStatus.INCREASING;
		}
		if (undefined == this.values.size()) {
			result = EvcsClusterStatus.UNDEFINED;
		}
		return result;
	}
}
