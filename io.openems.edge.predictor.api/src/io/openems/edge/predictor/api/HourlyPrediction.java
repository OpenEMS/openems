package io.openems.edge.predictor.api;

import java.time.LocalDateTime;

/**
 * Holds a prediction for 24 h; one value per hour; starting from 'start' time.
 */
public class HourlyPrediction {

	private final Integer[] values = new Integer[24];
	private final LocalDateTime start;
	
//	public HourlyPrediction() {
//		super();
//		for (int i = 0; i < 24 && i < values.length; i++) {
//			this.values[i] = 0;
//		}
//		this.start = LocalDateTime.now();
//	}

	public HourlyPrediction(Integer[] values, LocalDateTime start) {
		super();
		for (int i = 0; i < 24 && i < values.length; i++) {
			this.values[i] = values[i];
		}
		this.start = start;
	}

	public Integer[] getValues() {
		return values;
	}

	public LocalDateTime getStart() {
		return start;
	}

}
