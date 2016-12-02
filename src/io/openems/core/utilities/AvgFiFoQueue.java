package io.openems.core.utilities;

import com.google.common.collect.EvictingQueue;

public class AvgFiFoQueue {

	private EvictingQueue<Long> queue;

	public AvgFiFoQueue(int length) {
		queue = EvictingQueue.create(length);
	}

	public void add(long number) {
		queue.add(number);
	}

	public long avg() {
		long sum = 0;
		for (long value : queue) {
			sum += value;
		}
		if (sum == 0) {
			return 0;
		} else {
			return sum / queue.size();
		}
	}

}
