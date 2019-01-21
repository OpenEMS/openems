package io.openems.edge.common.channel.calculate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import io.openems.edge.common.channel.Channel;

/**
 * Helper class to calculate the average of Channel-Values.
 */
public class CalculateAverage {

	private final List<Double> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 * 
	 * @param channel
	 */
	public void addValue(Channel<Integer> channel) {
		Optional<Integer> value = channel.getNextValue().asOptional();
		if (value.isPresent()) {
			this.values.add(Double.valueOf(value.get()));
		}
	}

	/**
	 * Calculates the average.
	 * 
	 * @return the average or null
	 */
	public Double calculate() throws NoSuchElementException {
		if (this.values.isEmpty()) {
			return null;
		}
		return this.values //
				.stream() //
				.mapToDouble(value -> value) //
				.average() //
				.orElse(0.0);
	}
}
