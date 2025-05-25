package io.openems.backend.oem.fenecon;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

import io.openems.common.utils.ThreadPoolUtils;

// TODO move to common when stable
public class LatestTaskPerKeyExecutor<K> {

	private final ThreadPoolExecutor executor;
	private final Map<K, Runnable> latestTasks = new ConcurrentHashMap<>();
	private final AtomicInteger discardedTasks = new AtomicInteger();

	public LatestTaskPerKeyExecutor(ThreadPoolExecutor executor) {
		this.executor = executor;
	}

	/**
	 * Executes the task some time in the future and associates it with the key. If
	 * a new task gets scheduled with the same key the and last task had not been
	 * executed yet the last task gets dropped and only the new task gets executed.
	 * 
	 * @param key  the key to associate with the task
	 * @param task the task to execute
	 */
	public void execute(K key, Runnable task) {
		final var prevTask = this.latestTasks.put(key, task);
		if (prevTask != null) {
			this.discardedTasks.incrementAndGet();
			return;
		}
		this.executor.execute(() -> {
			this.latestTasks.remove(key).run();
		});
	}

	/**
	 * Initiates an orderly shutdown in which previously submitted tasks are
	 * executed, but no new tasks will be accepted.Invocation has no additional
	 * effect if already shut down.
	 * 
	 * @implNote just calls {@link ExecutorService#shutdown()} on the provided
	 *           {@link ExecutorService} in the constructor
	 * 
	 * @see ExecutorService#shutdown()
	 */
	public void shutdown() {
		this.executor.shutdown();
	}

	/**
	 * Attempts to stop all actively executing tasks, halts the processing of
	 * waiting tasks, and returns a list of the tasks that were awaiting execution.
	 * These tasks are drained (removed)from the task queue upon return from this
	 * method.
	 * 
	 * @implNote just calls {@link ExecutorService#shutdownNow()} on the provided
	 *           {@link ExecutorService} in the constructor
	 * 
	 * @see ExecutorService#shutdownNow()
	 */
	public void shutdownNow() {
		this.executor.shutdownNow();
	}

	/**
	 * Creates a debug log from this executor.
	 * 
	 * @return the debug log
	 */
	public String debugLog() {
		return ThreadPoolUtils.debugLog(this.executor) + ", DiscardedTasks: " + this.discardedTasks.get();
	}

}