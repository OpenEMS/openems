package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Executes subscriber every full Minute. Starts and stops itself, depending on
 * whether subscribers are present.
 */
public class MinuteTimer {
	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
			.setNameFormat("Alerting-MinuteTimer-%d").build();

	private final Logger log = LoggerFactory.getLogger(MinuteTimer.class);

	private ScheduledExecutorService scheduler;
	private final List<Runnable> subscriber;
	private ScheduledFuture<?> thread = null;

	private static MinuteTimer INSTANCE = new MinuteTimer();

	public static MinuteTimer getInstance() {
		return MinuteTimer.INSTANCE;
	}

	private MinuteTimer() {
		this.subscriber = new ArrayList<>();
	}

	/**
	 * Add subscriber for every minute execution.
	 *
	 * @param sub to add
	 */
	public void subscribe(Runnable sub) {
		this.subscriber.add(sub);
		if (this.scheduler == null) {
			this.start();
		}
	}

	/**
	 * Remove subscriber from every minute execution.
	 *
	 * @param sub to remove
	 */
	public void unsubscribe(Runnable sub) {
		if (sub == null) {
			return;
		}
		this.subscriber.remove(sub);
		if (this.subscriber.isEmpty()) {
			this.stop();
		}
	}

	private void start() {
		this.log.info("[Alerting-MinuteTimer] start");
		this.scheduler = Executors.newSingleThreadScheduledExecutor(threadFactory);
		this.cycle();
	}

	private void cycle() {
		this.log.debug("[Alerting-MinuteTimer] cycle");
		this.thread = this.scheduler.schedule(this::cycle, this.millisToNextMinute(), TimeUnit.MILLISECONDS);
		this.subscriber.forEach((sub) -> {
			try {
				sub.run();
			} catch (Throwable t) {
				this.log.error(t.getMessage(), t);
			}
		});
	}

	private void stop() {
		this.log.info("[Alerting-MinuteTimer] stop");
		this.thread.cancel(true);
		this.scheduler.shutdownNow();
		this.scheduler = null;
	}

	private int millisToNextMinute() {
		var nextMinute = ZonedDateTime.now().plusSeconds(2).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
		return (int) ChronoUnit.MILLIS.between(ZonedDateTime.now(), nextMinute);
	}
}
