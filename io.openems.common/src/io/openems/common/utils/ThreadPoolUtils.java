package io.openems.common.utils;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPoolUtils {

	private static final Logger LOG = LoggerFactory.getLogger(ThreadPoolUtils.class);

	private ThreadPoolUtils() {
	}

	/**
	 * Shutdown a {@link ExecutorService}.
	 *
	 * <p>
	 * Source:
	 * https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ExecutorService.html
	 *
	 * @param pool           the {@link ExecutorService} pool
	 * @param timeoutSeconds the applied timeout (is applied twice in the worst
	 *                       case)
	 */
	public static void shutdownAndAwaitTermination(ExecutorService pool, int timeoutSeconds) {
		if (pool == null) {
			return;
		}
		pool.shutdown(); // Disable new tasks from being submitted
		try {
			// Wait a while for existing tasks to terminate
			if (!pool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
				pool.shutdownNow(); // Cancel currently executing tasks
				// Wait a while for tasks to respond to being cancelled
				if (!pool.awaitTermination(timeoutSeconds, TimeUnit.SECONDS)) {
					ThreadPoolUtils.LOG.warn("Pool did not terminate");
				}
			}
		} catch (InterruptedException ie) {
			// (Re-)Cancel if current thread also interrupted
			pool.shutdownNow();
			// Preserve interrupt status
			Thread.currentThread().interrupt();
		}
	}

	/**
	 * Creates a debug log output with key metrics of the given
	 * {@link ThreadPoolExecutor}.
	 * 
	 * @param executor the executor
	 * @return a String
	 */
	public static String debugLog(ThreadPoolExecutor executor) {
		var activeCount = executor.getActiveCount();
		var b = new StringBuilder() //
				.append("Pool: ").append(executor.getPoolSize()).append("/").append(executor.getMaximumPoolSize()) //
				.append(", Pending: ").append(executor.getQueue().size()) //
				.append(", Completed: ").append(executor.getCompletedTaskCount()) //
				.append(", Active: ").append(activeCount); //
		if (executor.getMaximumPoolSize() == activeCount) {
			b.append(" !!!BACKPRESSURE!!!");
		}
		return b.toString();
	}

	/**
	 * Creates a map of debug metrics of the given {@link ThreadPoolExecutor}.
	 * 
	 * @param executor the executor
	 * @return a Map of key to value
	 */
	public static Map<String, Long> debugMetrics(ThreadPoolExecutor executor) {
		return Map.<String, Long>of(//
				"PoolSize", Long.valueOf(executor.getPoolSize()), //
				"MaxPoolSize", Long.valueOf(executor.getMaximumPoolSize()), //
				"Active", Long.valueOf(executor.getActiveCount()), //
				"Pending", Long.valueOf(executor.getQueue().size()), //
				"Completed", executor.getCompletedTaskCount() //
		);
	}

}
