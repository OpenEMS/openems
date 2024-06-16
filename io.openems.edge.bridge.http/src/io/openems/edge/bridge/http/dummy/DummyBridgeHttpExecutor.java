package io.openems.edge.bridge.http.dummy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.http.api.BridgeHttpExecutor;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;

public class DummyBridgeHttpExecutor implements BridgeHttpExecutor {

	private class Task implements ScheduledFuture<Object> {
		private final Instant start;
		private final Duration delay;
		private final Runnable runnable;
		private final CompletableFuture<?> doneFuture = new CompletableFuture<Object>();
		private boolean cancelled = false;

		public Task(Instant start, Duration delay, Runnable task) {
			super();
			this.start = start;
			this.delay = delay;
			this.runnable = task;
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return Duration.between(DummyBridgeHttpExecutor.this.clock.instant(), this.start.plus(this.delay))
					.get(unit.toChronoUnit());
		}

		@Override
		public int compareTo(Delayed o) {
			final var secondsCompareResult = Long.compare(this.getDelay(TimeUnit.SECONDS),
					o.getDelay(TimeUnit.SECONDS));
			if (secondsCompareResult != 0) {
				return secondsCompareResult;
			}
			return Long.compare(this.getDelay(TimeUnit.NANOSECONDS), o.getDelay(TimeUnit.NANOSECONDS));
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			if (this.cancelled) {
				return true;
			}
			if (this.isDone()) {
				return false;
			}
			DummyBridgeHttpExecutor.this.timePriorityTasks.remove(this);
			this.doneFuture.completeExceptionally(new Exception("Cancelled"));
			return this.cancelled = true;
		}

		@Override
		public boolean isCancelled() {
			return this.cancelled;
		}

		@Override
		public boolean isDone() {
			return this.doneFuture.isDone();
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return this.doneFuture.get();
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException, TimeoutException {
			return this.doneFuture.get(timeout, unit);
		}

		private void run() {
			try {
				this.runnable.run();
			} catch (Exception e) {
				DummyBridgeHttpExecutor.this.log.error("Task completed exceptionally.", e);
			} finally {
				this.doneFuture.complete(null);
			}
		}

	}

	private final Logger log = LoggerFactory.getLogger(DummyBridgeHttpExecutor.class);

	private final Clock clock;
	private final PriorityQueue<Task> timePriorityTasks = new PriorityQueue<Task>();
	private final TaskExecutor taskExecutor;
	private boolean shutdown = false;

	public DummyBridgeHttpExecutor(Clock clock, boolean handleTasksImmediately) {
		super();
		this.clock = clock;
		this.taskExecutor = handleTasksImmediately ? new ImmediateTaskExecutor() : new DelayedTaskExecutor();
	}

	public DummyBridgeHttpExecutor(Clock clock) {
		this(clock, false);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Delay.DurationDelay durationDelay) {
		if (this.isShutdown()) {
			throw new RuntimeException("Executor is shutdown.");
		}
		final var t = new Task(this.clock.instant(), durationDelay.getDuration(), task);
		if (!this.timePriorityTasks.offer(t)) {
			this.log.info("Unable to add Task to Queue.");
			return null;
		}
		return t;
	}

	@Override
	public void execute(Runnable task) {
		if (this.isShutdown()) {
			throw new RuntimeException("Executor is shutdown.");
		}
		this.taskExecutor.execute(task);
	}

	@Override
	public boolean isShutdown() {
		return this.shutdown;
	}

	/**
	 * Updates the executor. Executes all tasks which got queued up and for
	 * scheduled tasks executes these where its time elapsed based on the provided
	 * {@link Clock}.
	 */
	public void update() {
		this.taskExecutor.update();

		while (!this.timePriorityTasks.isEmpty() //
				&& this.timePriorityTasks.peek().getDelay(TimeUnit.SECONDS) <= 0) {
			final var task = this.timePriorityTasks.poll();
			task.run();
		}
	}

	/**
	 * Shuts down this executor.
	 */
	public void shutdown() {
		if (this.isShutdown()) {
			return;
		}
		this.shutdown = true;
		this.timePriorityTasks.forEach(t -> t.cancel(false));
	}

	private static interface TaskExecutor {
		public void execute(Runnable task);

		public void update();
	}

	private static class DelayedTaskExecutor implements TaskExecutor {
		private List<Runnable> instantTasks = new ArrayList<Runnable>();

		@Override
		public void execute(Runnable task) {
			this.instantTasks.add(task);
		}

		@Override
		public void update() {
			final var tasksToExecute = this.instantTasks;
			this.instantTasks = new ArrayList<>();
			tasksToExecute.forEach(Runnable::run);
		}

	}

	private static class ImmediateTaskExecutor implements TaskExecutor {

		@Override
		public void execute(Runnable task) {
			task.run();
		}

		@Override
		public void update() {
			// empty
		}

	}

}
