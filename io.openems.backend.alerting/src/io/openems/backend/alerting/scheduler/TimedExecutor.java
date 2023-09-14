package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

public interface TimedExecutor {

	public static class TimedTask implements Comparable<TimedTask> {
		protected final ZonedDateTime executeAt;
		protected final Consumer<ZonedDateTime> task;

		public TimedTask(ZonedDateTime executeAt, Consumer<ZonedDateTime> task) {
			this.executeAt = executeAt;
			this.task = task;
		}

		@Override
		public int compareTo(TimedTask o) {
			return this.executeAt.compareTo(o.executeAt);
		}
	}

	/**
	 * Execute the given task at the given dateTime.
	 * 
	 * @param at  time to execute at
	 * @param task task to execute
	 * 
	 * @return reference to Task as {@link TimedTask} Can be used to cancel the
	 *         task.
	 */
	public TimedTask schedule(ZonedDateTime at, Consumer<ZonedDateTime> task);

	/**
	 * Cancel {@link TimedTask}.
	 *
	 * @param task to remove
	 */
	public void cancel(TimedTask task);

	/**
	 * Get the Executors current {@link ZonedDateTime}. Should be used to calculate
	 * execution time.
	 * 
	 * @return current {@link ZonedDateTime}
	 */
	public ZonedDateTime now();

}
