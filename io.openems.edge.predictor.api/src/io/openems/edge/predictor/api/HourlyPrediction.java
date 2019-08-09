package io.openems.edge.predictor.api;

import java.time.LocalDateTime;


public class HourlyPrediction {

	//ArrayList<Integer> values = new ArrayList<Integer>(24);
	public Integer[] values = new Integer[24];
	public LocalDateTime start;

	
	public HourlyPrediction(Integer[] values, LocalDateTime start) {
		super();
		this.values = values;
		this.start = start;		
	}

}
