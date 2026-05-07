package io.openems.edge.common.channel.calculate;

import java.util.ArrayList;
import java.util.List;

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
	 * @param channel the Channel
	 */
	public void addValue(Channel<Long> channel) {
		if (channel == null) {
			return;
		}
		var value = channel.value().asOptional();
		if (value.isPresent()) {
			try {
				this.values.add(value.get());
			} catch (Exception e) {
				this.log.error("Adding Channel [" + channel.address() + "] value [" + value + "] failed. "
						+ e.getClass().getSimpleName() + ": " + e.getMessage());
			}
		}
	}

	/**
	 * Resets the calculator for reuse.
	 */
	public void reset() {
		this.values.clear();
	}

	/**
	 * Calculates the sum.
	 *
	 * @return the sum or null if no values were added
	 */
	public Long calculate() {
		if (this.values.isEmpty()) {
			return null;
		}

		long sum = 0L;
		for (var val : this.values) {
			if (val != null) {
				sum += val;
			}
		}
		return sum;
	}
}
