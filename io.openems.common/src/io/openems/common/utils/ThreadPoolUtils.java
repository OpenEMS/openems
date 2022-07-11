package io.openems.common.utils;

import java.util.concurrent.ExecutorService;
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

}
