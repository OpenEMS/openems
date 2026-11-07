package io.openems.common.utils;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.function.Consumer;

import org.slf4j.Logger;

import io.openems.common.function.Disposable;
import io.openems.common.logger.ContextLogger;

public class ThrottledLoopExecutor implements Disposable {
	private final Throttler throttler;
	private final Logger log;
	private final Thread executorThread;

	private volatile boolean isRunning = true;
	private final Queue<QueuedTask<?>> tasksQueue = new ArrayDeque<>();

	/**
	 * Creates a new ThrottledLoopExecutor with a virtual thread.
	 *
	 * @param name      the name of the executor, used for logging and thread naming
	 * @param throttler the throttling strategy to control execution rate
	 */
	public ThrottledLoopExecutor(String name, Throttler throttler) {
		this.throttler = throttler;

		String logName = name + "::ThrottledLoopExecutor";
		this.log = new ContextLogger(ThrottledLoopExecutor.class, logName);

		this.executorThread = Thread.ofVirtual().name(logName).start(this::threadRun);
	}

	/**
	 * Adds a batch of items to be processed by the executor with throttling
	 * applied.
	 *
	 * <p>
	 * Items are processed sequentially in a separate virtual thread. The throttler
	 * controls the execution rate between processing each item. If the executor is
	 * not running, this call is ignored.
	 *
	 * @param <T>  the type of items to process
	 * @param list the collection of items to process
	 * @param func the consumer function to apply to each item
	 */
	public synchronized <T> void add(List<T> list, Consumer<T> func) {
		if (!this.isRunning || list.isEmpty()) {
			return;
		}

		this.tasksQueue.add(new QueuedTask<>(list, func));
		this.notifyAll();
	}

	private void threadRun() {
		while (Thread.currentThread().isAlive() && this.isRunning) {
			try {
				QueuedTask<?> task;
				synchronized (this) {
					task = this.tasksQueue.poll();
					if (task == null) {
						this.wait();
						continue;
					}
				}

				this.executeTask(task);
			} catch (InterruptedException ex) {
				// Ignore
			} catch (Exception ex) {
				this.log.error("Internal error in ThreadLoopExecutor", ex);
			}
		}
	}

	private <T> void executeTask(QueuedTask<T> task) throws InterruptedException {
		for (T obj : task.list) {
			try {
				this.throttler.throttle();
			} catch (InterruptedException ex) {
				if (!this.isRunning) {
					throw ex;
				}
			}

			try {
				task.func.accept(obj);
			} catch (RuntimeException ex) {
				this.log.error("Error executing task for function {} and object {}", task.func.getClass().getName(),
						obj, ex);
			}
		}
	}

	/**
	 * Stops the executor and terminates its virtual thread.
	 *
	 * <p>
	 * After calling this method, no new tasks will be accepted. The underlying
	 * thread will be interrupted to ensure clean shutdown.
	 */
	@Override
	public void dispose() {
		this.isRunning = false;
		this.executorThread.interrupt();
	}

	record QueuedTask<T>(Collection<T> list, Consumer<T> func) {
	}

	/**
	 * Strategy for controlling the execution rate of tasks in the
	 * ThrottledLoopExecutor.
	 */
	public interface Throttler {
		/**
		 * Executed every iteration by the Executor. This method is responsible to sleep
		 * to slowdown the executor.
		 * 
		 * @throws InterruptedException Thrown if sleep was interrupted.
		 */
		void throttle() throws InterruptedException;
	}

	/**
	 * Throttler implementation that limits execution to a maximum number of
	 * operations within a specified time duration.
	 *
	 * <p>
	 * The throttler maintains a rate limit by sleeping when the maximum number of
	 * executions has been reached within the current time window.
	 * </p>
	 */
	public static class MaxExecutionsPerDurationThrottler implements Throttler {
		private final Duration duration;
		private final int executionsPerDuration;

		private long lastReset = 0;
		private int executionsSinceLastReset = 0;

		public MaxExecutionsPerDurationThrottler(Duration duration, int executionsPerDuration) {
			this.duration = duration;
			this.executionsPerDuration = executionsPerDuration;
		}

		private void reset() {
			this.lastReset = System.currentTimeMillis();
			this.executionsSinceLastReset = 0;
		}

		@Override
		public void throttle() throws InterruptedException {
			long elapsedMillis = System.currentTimeMillis() - this.lastReset;
			if (elapsedMillis >= this.duration.toMillis()) {
				this.reset();
			}

			if (this.executionsSinceLastReset >= this.executionsPerDuration) {
				Thread.sleep(this.duration.toMillis() - elapsedMillis);
				this.reset();
			}

			this.executionsSinceLastReset++;
		}
	}

}
