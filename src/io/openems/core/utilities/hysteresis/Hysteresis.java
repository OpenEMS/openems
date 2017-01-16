package io.openems.core.utilities.hysteresis;

import io.openems.core.utilities.AvgFiFoQueue;

public class Hysteresis {

	private final Long min;
	private final Long max;
	private AvgFiFoQueue queue = new AvgFiFoQueue(10);

	public Hysteresis(long min, long max) {
		this.min = min;
		this.max = max;
	}

	public void apply(long value, HysteresisFunctional func) {
		if (queue.lastAddedValue() == null || queue.lastAddedValue() != value) {
			queue.add(value);
		}
		long pos = queue.avg() - min;
		// Check if value is in hysteresis
		double multiplier = (double) pos / (double) (max - min);
		HysteresisState state = HysteresisState.ABOVE;
		if (pos >= 0 && queue.avg() <= max) {
			// in Hysteresis
			if (queue.avg() < value) {
				// Ascending
				state = HysteresisState.ASC;
			} else {
				// Descending
				state = HysteresisState.DESC;
			}
		} else if (pos < 0) {
			// Below Hysteresis
			state = HysteresisState.BELOW;
		}
		func.function(state, multiplier);
	}

}
