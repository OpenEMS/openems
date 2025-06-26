package io.openems.edge.common.channel.calculate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;

/**
 * Helper class to sum up Integer-Channels.
 */
public class CalculateIntegerSum {

	public static final Function<Integer, Integer> DIRECT_CONVERTER = value -> value;
	public static final Function<Integer, Integer> DIVIDE_BY_THREE = value -> Math.round(value / 3f);

	private final Logger log = LoggerFactory.getLogger(CalculateLongSum.class);
	private final List<Integer> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 *
	 * @param channel the Channel
	 */
	public void addValue(Channel<Integer> channel) {
		this.addValue(channel, DIRECT_CONVERTER);
	}

	/**
	 * Adds a Channel-Value.
	 *
	 * @param channel   the Channel
	 * @param converter is applied to the channel value
	 */
	public void addValue(Channel<Integer> channel, Function<Integer, Integer> converter) {
		var value = channel.value().asOptional();
		if (value.isPresent()) {
			try {
				this.values.add(converter.apply(value.get()));
			} catch (Exception e) {
				this.log.error("Adding Channel [" + channel.address() + "] value [" + value + "] failed. "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Calculates the sum.
	 *
	 * @return the sum or null
	 * @throws NoSuchElementException on error
	 */
	public Integer calculate() throws NoSuchElementException {
		if (this.values.isEmpty()) {
			return null;
		}
		return this.values //
				.stream() //
				.mapToInt(value -> value) //
				.sum(); //
	}
}
