package io.openems.api.bridge;

import com.google.common.collect.EvictingQueue;

public abstract class BridgeTask {

	private EvictingQueue<Long> requiredTimes;

	public BridgeTask() {
		requiredTimes = EvictingQueue.create(5);
		for (int i = 0; i < 5; i++) {
			requiredTimes.add(0L);
		}
	}

	public long getRequiredTime() {
		long sum = 0;
		for (Long l : requiredTimes) {
			sum += l;
		}
		return sum / requiredTimes.size();
	}

	public void runTask() throws Exception {
		long timeBefore = System.currentTimeMillis();
		this.run();
		this.requiredTimes.add(System.currentTimeMillis() - timeBefore);
	}

	protected abstract void run() throws Exception;

}
