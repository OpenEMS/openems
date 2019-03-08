package io.openems.edge.common.channel.calculate;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.channel.Channel;

/**
 * Helper class to sum up Long-Channels.
 */
public class CalculateLongSum {

	private final Logger log = LoggerFactory.getLogger(CalculateLongSum.class);
	private final List<Long> values = new ArrayList<>();

	/**
	 * Adds a Channel-Value.
	 * 
	 * @param channel
	 */
	public void addValue(Channel<Long> channel) {
		Optional<Long> value = channel.getNextValue().asOptional();
		if (value.isPresent()) {
			try {
				this.values.add(value.get());
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
	 */
	public Long calculate() throws NoSuchElementException {
		if (this.values.isEmpty()) {
			return null;
		}
		return this.values //
				.stream() //
				.mapToLong(value -> value) //
				.sum(); //
	}
}
