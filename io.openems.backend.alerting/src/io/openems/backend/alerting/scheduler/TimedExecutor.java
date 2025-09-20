package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.function.Consumer;

public interface TimedExecutor {

	class TimedTask implements Comparable<TimedTask> {
		protected final ZonedDateTime executeAt;
		protected final Consumer<ZonedDateTime> task;

		public TimedTask(ZonedDateTime executeAt, Consumer<ZonedDateTime> task) {
			this.executeAt = executeAt;
			this.task = task;
		}

		@Override
		public int compareTo(TimedTask other) {
			if (other == null || other.executeAt == null) {
				return 1;
			}
			return this.executeAt.compareTo(other.executeAt);
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof TimedTask other
					&& this.executeAt.equals(other.executeAt)
					&& this.task.equals(other.task);
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.executeAt, this.task);
		}
	}

	/**
	 * Execute the given task at the given dateTime.
	 *
	 * @param at   time to execute at
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
