package io.openems.backend.alerting.scheduler;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Implementation of {@link MinuteTimer} using an
 * {@link ScheduledExecutorService} and the systems default {@link Clock} for
 * asynchronous execution.
 *
 */
public class MinuteTimerAsync extends MinuteTimer {

	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("Alerting-MinuteTimer-%d").build();

	private ScheduledExecutorService scheduler;

	public MinuteTimerAsync() {
		super(Clock.systemDefaultZone());
	}

	@Override
	protected void start() {
		super.start();
		this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
		this.scheduler.scheduleAtFixedRate(this::cycle, 0, 1, TimeUnit.MINUTES);
	}

	@Override
	public void subscribe(Consumer<ZonedDateTime> sub) {
		super.subscribe(sub);
		if (this.scheduler == null) {
			this.start();
		}
	}

	@Override
	public void unsubscribe(Consumer<ZonedDateTime> sub) {
		super.unsubscribe(sub);
		if (super.getSubscriberCount() == 0) {
			this.stop();
		}
	}

	@Override
	protected void stop() {
		super.stop();
		if (this.scheduler == null) {
			return;
		}
		this.scheduler.shutdownNow();
		this.scheduler = null;
	}
}
