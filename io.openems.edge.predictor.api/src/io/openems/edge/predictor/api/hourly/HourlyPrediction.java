package io.openems.edge.predictor.api.hourly;

import java.time.ZonedDateTime;

/**
 * Holds a prediction for 24 h; one value per hour; starting from 'start' time.
 */
@Deprecated
public class HourlyPrediction {

	private final Integer[] values = new Integer[24];
	private final ZonedDateTime start;

	public HourlyPrediction(Integer[] values, ZonedDateTime start) {
		super();
		for (int i = 0; i < 24 && i < values.length; i++) {
			this.values[i] = values[i];
		}
		this.start = start;
	}

	public Integer[] getValues() {
		return values;
	}

	public ZonedDateTime getStart() {
		return start;
	}

}
