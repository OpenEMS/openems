package io.openems.edge.bridge.eebus;

import io.openems.common.function.Disposable;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DelayedUpdater {
	private final Runnable runnable;
	private final Instant initializationTime;
	private final Duration updateDelay;
	private final Duration maxWaitBetweenUpdatesDelay;
	private final Object updateLock = new Object();

	private volatile Object updateToken = new Object();
	private Instant lastExecution;

	public DelayedUpdater(Runnable runnable, Duration initialDelay, Duration updateDelay, Duration maxWaitBetweenUpdatesDelay) {
		this.runnable = runnable;
		this.initializationTime = Instant.now();
		this.updateDelay = updateDelay;
		this.maxWaitBetweenUpdatesDelay = maxWaitBetweenUpdatesDelay;
		if (initialDelay != null) {
			this.scheduleRun(initialDelay);
		}
	}

	private void scheduleRun(Duration delay) {
		var myUpdateToken = new Object();
		this.updateToken = myUpdateToken;

		var forceExecutionTime = this.lastExecution != null //
				? this.lastExecution.plus(this.maxWaitBetweenUpdatesDelay) //
				: this.initializationTime.plus(this.maxWaitBetweenUpdatesDelay);

		if (Instant.now().isAfter(forceExecutionTime)) {
			CompletableFuture.runAsync(() -> this.delayedRun(myUpdateToken));
		} else {
			CompletableFuture.delayedExecutor(delay.get(ChronoUnit.SECONDS), TimeUnit.SECONDS)
					.execute(() -> this.delayedRun(myUpdateToken));
		}
	}

	private void delayedRun(Object updateToken) {
		synchronized (this.updateLock) {
			if (this.updateToken != updateToken) {
				return;
			}

			this.lastExecution = Instant.now();
			this.runnable.run();
		}
	}

	public void scheduleUpdate() {
		this.scheduleRun(this.updateDelay);
	}

	public void cancelPendingUpdate() {
		this.updateToken = new Object();
	}
}
