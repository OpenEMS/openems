package io.openems.edge.energy.optimizer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class OptimizerThreadFactory implements ThreadFactory {
	private static final ConcurrentMap<String, AtomicInteger> poolCounters = new ConcurrentHashMap<>();
	private static final ThreadGroup threadGroup = new ThreadGroup("Optimizer");

	public static final String KEY_OPTIMIZER_EXECUTOR = "Simulator[Executor]";
	public static final String KEY_OPTIMIZER_SCHEDULER =  "Simulator[Scheduler]";
	public static final String KEY_JENETICS = "Simulator[Jenetics]";

	private final String key;
	private final int priority;

	private final AtomicInteger threadCounter = new AtomicInteger(0);
	private final int poolCount;

	public OptimizerThreadFactory(String key, int priority) {
		this.key = key;
		this.priority = priority;
		this.poolCount = poolCounters.computeIfAbsent(key, k -> new AtomicInteger(0)).incrementAndGet();
	}

	private String getNameForNewThread() {
		var threadCount = this.threadCounter.incrementAndGet();
		return String.format("%s[%02d]-%d", this.key, this.poolCount, threadCount);
	}

	@Override
	public Thread newThread(Runnable task) {
		var name = this.getNameForNewThread();
		var thread = new Thread(threadGroup, task, name);
		thread.setDaemon(true);
		thread.setPriority(this.priority);
		return thread;
	}
}
