package io.openems.api.bridge;

import java.util.Queue;

import com.google.common.collect.EvictingQueue;
import com.google.common.collect.Queues;

public abstract class BridgeTask {

	private Queue<Long> requiredTimes;

	public BridgeTask() {
		requiredTimes = EvictingQueue.create(5);
		for (int i = 0; i < 5; i++) {
			requiredTimes.add(0L);
		}
	}

	public long getRequiredTime() {
		synchronized (requiredTimes) {
			long sum = 0;
			for (Long l : requiredTimes) {
				sum += l;
			}
			return sum / requiredTimes.size();
		}
	}

	public void runTask() throws Exception {
		long timeBefore = System.currentTimeMillis();
		this.run();
		synchronized (requiredTimes) {
			this.requiredTimes.add(System.currentTimeMillis() - timeBefore);
		}
	}

	protected abstract void run() throws Exception;

}
