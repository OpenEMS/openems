package io.openems.common.bridge.http;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.utils.ThreadPoolUtils;

@Component(scope = ServiceScope.PROTOTYPE)
public class AsyncBridgeHttpExecutor implements BridgeHttpExecutor {

	/**
	 * A Semaphore that allows setting the total number of permits.
	 * 
	 * <p>
	 * This requires that all threads acquire and release an equal amount of
	 * permits. Otherwise, the number of actual permits will drift.
	 */
	private class MySemaphore extends Semaphore {
		private static final long serialVersionUID = 1122515497359767441L;

		private int totalPermits;

		public MySemaphore(int permits) {
			this(permits, false);
		}

		public MySemaphore(int permits, boolean fair) {
			super(permits, fair);
			this.totalPermits = permits;
		}

		public void setTotalPermits(int totalPermits) {
			if (totalPermits >= this.totalPermits) {
				this.release(totalPermits - this.totalPermits);
			} else {
				this.reducePermits(this.totalPermits - totalPermits);
			}
			this.totalPermits = totalPermits;
		}
	}

	private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
	private final MySemaphore semaphore = new MySemaphore(10);

	private final AtomicLong activeCounter = new AtomicLong();
	private final AtomicLong completedCounter = new AtomicLong();

	@Override
	public ScheduledFuture<?> schedule(Runnable task, DelayTimeProvider.Delay.DurationDelay durationDelay) {
		return this.scheduler.schedule(() -> this.execute(task), durationDelay.getDuration().toMillis(),
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void execute(Runnable task) {
		this.executor.execute(() -> {
			try {
				this.semaphore.acquire();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return;
			}

			try {
				this.activeCounter.incrementAndGet();
				task.run();
			} finally {
				this.semaphore.release();
				this.activeCounter.decrementAndGet();
				this.completedCounter.incrementAndGet();
			}
		});
	}

	@Override
	public boolean isShutdown() {
		return this.scheduler.isShutdown() && this.executor.isShutdown();
	}

	@Deactivate
	protected void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 0);
		ThreadPoolUtils.shutdownAndAwaitTermination(this.executor, 0);
	}

	@Override
	public Map<String, Long> getMetrics() {
		return Map.of(//
				"Permits", (long) this.semaphore.availablePermits(), //
				"Active", this.activeCounter.get(), //
				"Pending", (long) this.semaphore.getQueueLength(), //
				"Completed", this.completedCounter.get());
	}

	@Override
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.semaphore.setTotalPermits(maximumPoolSize);
	}

}
