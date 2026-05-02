package io.openems.common.bridge.http.time.periodic;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.bridge.http.time.DelayTimeProviderChain;
import io.openems.common.utils.ThreadPoolUtils;

public class PeriodicExecutorImpl implements PeriodicExecutor {
	private final Logger log = LoggerFactory.getLogger(PeriodicExecutorImpl.class);
	private final String name;
	private final ScheduledThreadPoolExecutor pool;
	private final Supplier<DelayTimeProvider.Delay> action;

	private ScheduledFuture<?> currentTask;

	public PeriodicExecutorImpl(String name, Supplier<DelayTimeProvider.Delay> action,
			DelayTimeProvider.Delay firstExecutionDelay) {
		this.name = name;
		this.pool = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().name(name).factory());
		this.pool.setMaximumPoolSize(1);
		this.action = action;

		this.scheduleNextRun(firstExecutionDelay);
	}

	@Override
	public long getRemainingDelayUntilNextRun(TimeUnit unit) {
		if (this.currentTask == null) {
			return -1;
		}
		return this.currentTask.getDelay(unit);
	}

	private void logError(String message, Exception exception) {
		this.log.error("[%s]: %s".formatted(this.name, message), exception);
	}

	protected void run() {
		DelayTimeProvider.Delay delay;
		try {
			delay = this.action.get();
		} catch (Exception ex) {
			this.logError("An exception occurred while periodic task execution", ex);

			// On error, retry after 1 minute with some randomness
			delay = DelayTimeProviderChain.fixedDelay(Duration.ofMinutes(1)) //
					.plusRandomDelay(30, ChronoUnit.SECONDS) //
					.getDelay();
		}

		this.scheduleNextRun(delay);
	}

	protected void scheduleNextRun(DelayTimeProvider.Delay delay) {
		var durationDelay = (DelayTimeProvider.Delay.DurationDelay) delay;
		this.currentTask = this.pool.schedule(this::run, durationDelay.getDuration().toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void dispose() {
		if (this.currentTask != null) {
			this.currentTask.cancel(false);
			this.currentTask = null;
		}

		ThreadPoolUtils.shutdownAndAwaitTermination(this.pool, 0);
	}
}
