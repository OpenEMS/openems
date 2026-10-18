package io.openems.edge.controller.api.backend.syncable;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import io.openems.edge.controller.api.backend.api.ControllerApiBackend;

public class MockBackendSyncableRepository extends BackendSyncableRepository {
	private final List<Future<?>> asyncTasks = new LinkedList<>();

	MockBackendSyncableRepository(ControllerApiBackend backend) {
		this.bindBackend(backend);
	}

	@Override
	protected <T> void startHandlerUnsubscribeTimer(SubscriptionHandler<T> handler) {
		this.unsubscribeHandler(handler);
	}

	@Override
	protected synchronized void runAsync(Runnable r) {
		if (this.executor == null) {
			return;
		}

		synchronized (this.asyncTasks) {
			this.asyncTasks.add(this.executor.submit(r));
		}
	}

	/**
	 * Waits for all asynchronous tasks to finish execution.
	 *
	 * @throws ExecutionException   Called if an exception occurred during the
	 *                              execution of a task.
	 * @throws InterruptedException Called if the current thread was interrupted
	 *                              while waiting for a task to complete.
	 */
	public void ensureThatAllAsyncTasksAreExecuted() throws ExecutionException, InterruptedException {
		Future<?>[] tasks;
		synchronized (this.asyncTasks) {
			tasks = this.asyncTasks.toArray(Future<?>[]::new);
			this.asyncTasks.clear();
		}

		for (var task : tasks) {
			task.get();
		}
	}
}
