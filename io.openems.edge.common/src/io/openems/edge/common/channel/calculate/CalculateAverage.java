package io.openems.edge.common.channel.calculate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;

/**
 * Helper class to calculate the average of Channel-Values.
 */
public class CalculateAverage {

	private final Logger log = LoggerFactory.getLogger(CalculateAverage.class);
	private final List<Double> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 *
	 * @param channel the channel
	 */
	public void addValue(Channel<? extends Number> channel) {
		var value = channel.value().asOptional();
		if (value.isPresent()) {
			try {
				this.values.add(value.get().doubleValue());
			} catch (Exception e) {
				this.log.error("Adding Channel [" + channel.address() + "] value [" + value + "] failed. "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
				e.printStackTrace();
			}
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

	/**
	 * Calculates the average and rounds to Integer.
	 *
	 * @return the average or null
	 */
	public Integer calculateRounded() throws NoSuchElementException {
		var value = this.calculate();
		if (value == null) {
			return null;
		}
		var longValue = Math.round(value);
		if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
			return (int) longValue;
		}
		throw new IllegalArgumentException("Cannot convert. Double [" + value + "] is not fitting in Integer range.");
	}
}
