package io.openems.edge.greenconsumptionadvisor.api;

import java.time.LocalDateTime;

public class GridEmissionInformation {
	
	private LocalDateTime timestamp;
	private ConsumptionAdvice advice;
	private int co2Emissions;
	
	public GridEmissionInformation(LocalDateTime timestamp, ConsumptionAdvice advice, int co2Emissions) {
		this.timestamp = timestamp;
		this.advice = advice;
		this.co2Emissions = co2Emissions;
	}
	
	public LocalDateTime getTimestamp() {
		return this.timestamp;
	}
	
	public ConsumptionAdvice getConsumptionAdvice()	{
		return this.advice;
	}

	public int getCo2Emissions() {
		return this.co2Emissions;
	}
}
