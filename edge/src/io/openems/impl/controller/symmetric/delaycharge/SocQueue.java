package io.openems.impl.controller.symmetric.delaycharge;

import java.util.Optional;

import com.google.common.collect.EvictingQueue;

public class SocQueue {

	private class TimedValue {
		final long value;
		final long secondOfDay;

		public TimedValue(long value) {
			this.value = value;
			this.secondOfDay = Util.currentSecondOfDay();
		}

		@Override
		public String toString() {
			return "[" + this.secondOfDay + ":" + this.value + "]";
		}
	}

	private EvictingQueue<TimedValue> queue;
	private Optional<TimedValue> newestValue = Optional.empty();

	public SocQueue(int length) {
		this.queue = EvictingQueue.create(length);
	}

	public synchronized void addIfChanged(long value) {
		TimedValue thisValue = new TimedValue(value);
		if (!this.newestValue.isPresent() || this.newestValue.get().value != value) {
			this.queue.add(thisValue);
		}
		this.newestValue = Optional.of(thisValue);
	}

	/**
	 * Returns the gradient of the queue in relation to the minuteOfDay
	 */
	public synchronized double getGradient() {
		// verify that we have values
		Optional<TimedValue> newestOpt = this.getNewestValue();
		Optional<TimedValue> oldestOpt = this.getOldestValue();
		if (!newestOpt.isPresent() || !oldestOpt.isPresent()) {
			return 0;
		}
		TimedValue newest = newestOpt.get();
		TimedValue oldest = oldestOpt.get();

		// calculate gradient
		if (oldest.secondOfDay == newest.secondOfDay) {
			return 0; // avoid division by zero
		}
		double gradient = (double) (newest.value - oldest.value) / (newest.secondOfDay - oldest.secondOfDay);
		return gradient;
	}

	@Override
	public String toString() {
		return this.queue.toString();
	}

	public Optional<TimedValue> getOldestValue() {
		return Optional.ofNullable(this.queue.peek());
	}

	public Optional<TimedValue> getNewestValue() {
		return this.newestValue;
	}
}
