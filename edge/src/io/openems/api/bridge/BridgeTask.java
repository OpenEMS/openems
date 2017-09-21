package io.openems.api.bridge;

import java.util.Collections;
import java.util.Queue;

import com.google.common.collect.EvictingQueue;

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
			return Collections.max(requiredTimes);
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
