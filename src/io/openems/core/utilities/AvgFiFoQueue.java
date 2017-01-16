package io.openems.core.utilities;

import com.google.common.collect.EvictingQueue;

public class AvgFiFoQueue {

	private EvictingQueue<Long> queue;
	private Long lastValue;

	public AvgFiFoQueue(int length) {
		queue = EvictingQueue.create(length);
	}

	public void add(long number) {
		queue.add(number);
	}

	public long avg() {
		long sum = 0;
		long multiplier = 1;
		long divisor = 0;
		for (long value : queue) {
			sum += value * multiplier;
			divisor += multiplier;
			multiplier += multiplier;
		}
		if (sum == 0) {
			return 0;
		} else {
			return sum / divisor;
		}
	}

	public Long lastAddedValue() {
		return lastValue;
	}

}
