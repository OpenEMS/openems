package io.openems.edge.controller.timelinecharge;

import com.google.common.collect.EvictingQueue;

public class AvgFiFoQueue {

	private EvictingQueue<Long> queue;
	private Long lastValue;
	private double gradeient;

	public AvgFiFoQueue(int length, double gradient) {
		queue = EvictingQueue.create(length);
		this.gradeient = gradient;
	}

	public void add(long number) {
		queue.add(number);
		lastValue = number;
	}

	public long avg() {
		long sum = 0;
		double multiplier = 1;
		double divisor = 0;
		for (long value : queue) {
			sum += value * multiplier;
			divisor += multiplier;
			multiplier = (long) ((multiplier) * gradeient);
		}
		if (sum == 0) {
			return 0;
		} else {
			return (long) (sum / divisor);
		}
	}

	public Long lastAddedValue() {
		return lastValue;
	}

	@Override
	public String toString() {
		return "Avg: " + avg();
	}

}
