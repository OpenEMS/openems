package io.openems.common.utils;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RejectedExecutionException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Executes at most one asynchronous task at a time and keeps only the latest
 * task submitted while another task is running. Intermediate pending tasks are
 * discarded.
 */
public final class LatestWinsFutureExecutor {

	private final CancellationToken cancellationToken = new CancellationToken();

	private boolean taskInFlight = false;
	private Runnable pendingTask = null;

	/**
	 * Executes the task immediately or retains it as the latest pending task.
	 *
	 * @param task       starts the asynchronous task; it must return without
	 *                   waiting for task completion
	 * @param onComplete handles the task result or error
	 * @param <R>        the result type
	 * @throws RejectedExecutionException if this executor is cancelled
	 */
	public synchronized <R> void execute(Supplier<CompletableFuture<R>> task, BiConsumer<R, Throwable> onComplete) {
		Objects.requireNonNull(task);
		Objects.requireNonNull(onComplete);
		if (this.cancellationToken.isCancelled()) {
			throw new RejectedExecutionException("Executor is cancelled");
		}

		final Runnable executableTask = () -> this.startTask(task, onComplete);
		if (this.taskInFlight) {
			this.pendingTask = executableTask;
			return;
		}
		this.taskInFlight = true;
		executableTask.run();
	}

	/**
	 * Cancels this executor. New values are rejected, pending work is discarded and
	 * completions of already running work are ignored.
	 */
	public synchronized void cancel() {
		this.cancellationToken.cancel();
		this.taskInFlight = false;
		this.pendingTask = null;
	}

	private synchronized <R> void startTask(Supplier<CompletableFuture<R>> task, BiConsumer<R, Throwable> onComplete) {
		final CompletableFuture<R> future;
		try {
			future = Objects.requireNonNull(task.get());
		} catch (Exception e) {
			this.handleCompletion(null, e, onComplete);
			return;
		}
		future.whenComplete((result, error) -> this.handleCompletion(result, error, onComplete));
	}

	private synchronized <R> void handleCompletion(R result, Throwable error, BiConsumer<R, Throwable> onComplete) {
		if (this.cancellationToken.isCancelled()) {
			return;
		}
		try {
			onComplete.accept(result, error);
		} finally {
			this.finishTask();
		}
	}

	private synchronized void finishTask() {
		if (this.cancellationToken.isCancelled()) {
			return;
		}
		final var nextTask = this.pendingTask;
		this.pendingTask = null;
		if (nextTask == null) {
			this.taskInFlight = false;
			return;
		}
		nextTask.run();
	}
}
