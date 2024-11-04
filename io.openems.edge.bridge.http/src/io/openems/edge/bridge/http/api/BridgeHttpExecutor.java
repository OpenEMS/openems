package io.openems.edge.bridge.http.api;

import java.util.concurrent.ScheduledFuture;

import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;

/**
 * Executor to handle tasks created by a {@link BridgeHttp}.
 */
public interface BridgeHttpExecutor {

	/**
	 * Schedules a task to be executed from now plus the given delay.
	 * 
	 * @param task          the task to execute
	 * @param durationDelay the delay to schedule toe task
	 * @return a {@link ScheduledFuture}
	 */
	public ScheduledFuture<?> schedule(Runnable task, Delay.DurationDelay durationDelay);

	/**
	 * Executes the given task.
	 * 
	 * @param task the task to execute
	 */
	public void execute(Runnable task);

	/**
	 * Determines if this executor is shutdown.
	 * 
	 * @return true if this executor is shutdown else false
	 */
	public boolean isShutdown();

}
