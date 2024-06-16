package io.openems.edge.bridge.http;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.ServiceScope;

import io.openems.common.utils.ThreadPoolUtils;
import io.openems.edge.bridge.http.api.BridgeHttpExecutor;
import io.openems.edge.bridge.http.time.DelayTimeProvider.Delay;

@Component(scope = ServiceScope.PROTOTYPE)
public class AsyncBridgeHttpExecutor implements BridgeHttpExecutor {

	// TODO change to java 21 virtual threads
	private final ScheduledExecutorService pool = Executors.newScheduledThreadPool(0);

	@Override
	public ScheduledFuture<?> schedule(Runnable task, Delay.DurationDelay durationDelay) {
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

}
