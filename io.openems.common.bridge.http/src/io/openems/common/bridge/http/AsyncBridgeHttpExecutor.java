package io.openems.common.bridge.http;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.bridge.http.api.BridgeHttpExecutor;
import io.openems.common.bridge.http.time.DelayTimeProvider;
import io.openems.common.utils.ThreadPoolUtils;

@Component(scope = ServiceScope.PROTOTYPE)
public class AsyncBridgeHttpExecutor implements BridgeHttpExecutor {

	private final ScheduledThreadPoolExecutor pool = new ScheduledThreadPoolExecutor(0, Thread.ofVirtual().factory());

	public AsyncBridgeHttpExecutor() {
		// set the default maximum pool size
		this.pool.setMaximumPoolSize(10);
	}

	@Override
	public ScheduledFuture<?> schedule(Runnable task, DelayTimeProvider.Delay.DurationDelay durationDelay) {
		return this.pool.schedule(task, durationDelay.getDuration().toMillis(), TimeUnit.MILLISECONDS);
	}

	@Override
	public void execute(Runnable task) {
		this.pool.execute(task);
	}

	@Override
	public boolean isShutdown() {
		return this.pool.isShutdown();
	}

	@Deactivate
	private void deactivate() {
		ThreadPoolUtils.shutdownAndAwaitTermination(this.pool, 0);
	}

	@Override
	public Map<String, Long> getMetrics() {
		return ThreadPoolUtils.debugMetrics(this.pool);
	}

	@Override
	public void setMaximumPoolSize(int maximumPoolSize) {
		this.pool.setMaximumPoolSize(maximumPoolSize);
	}

}
