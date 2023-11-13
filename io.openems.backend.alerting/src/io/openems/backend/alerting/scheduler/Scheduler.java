package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;

/**
 * A {@link MessageSchedulerService} implementation using {@link MinuteTimer}.
 */
public class Scheduler implements Consumer<ZonedDateTime>, MessageSchedulerService, TimedExecutor {

	private final MinuteTimer minuteTimer;
	private final List<MessageScheduler<? extends Message>> msgScheduler;

	private final Logger log = LoggerFactory.getLogger(Scheduler.class);

	public Scheduler(MinuteTimer timer) {
		this.minuteTimer = timer;
		this.msgScheduler = new ArrayList<>();
	}

	public Scheduler() {
		this(new MinuteTimerAsync());
	}

	@Override
	public <T extends Message> MessageScheduler<T> register(Handler<T> handler) {
		var msgSch = new MessageScheduler<>(handler);
		this.msgScheduler.add(msgSch);
		return msgSch;
	}

	@Override
	public <T extends Message> void unregister(Handler<T> handler) {
		this.msgScheduler.removeIf(msgs -> msgs.isFor(handler));
	}

	@Override
	public TimedTask schedule(ZonedDateTime at, Consumer<ZonedDateTime> task) {
		return this.minuteTimer.schedule(at, task);
	}

	@Override
	public void cancel(TimedTask task) {
		this.minuteTimer.cancel(task);
	}

	@Override
	public ZonedDateTime now() {
		return this.minuteTimer.now();
	}

	/**
	 * Subscribe to minuteTimer and start scheduling.
	 */
	public void start() {
		this.log.info("[Alerting-Scheduler] start");
		this.minuteTimer.subscribe(this);
	}

	/**
	 * Unsubscribe from minuteTimer and stop scheduling.
	 */
	public void stop() {
		this.log.info("[Alerting-Scheduler] stop");
		this.minuteTimer.unsubscribe(this);
	}

	/**
	 * Get the total count of Messages scheduled for dispatch.
	 *
	 * @return total count
	 */
	public int getScheduledMsgsCount() {
		return this.msgScheduler.stream().map(MessageScheduler::size).reduce(0, Integer::sum);
	}

	/**
	 * Check if given message is scheduled within any MessageScheduler.
	 *
	 * @param msg to check for
	 * @return true if any scheduling was found
	 */
	public boolean isScheduled(Message msg) {
		return this.msgScheduler.stream().anyMatch(ms -> ms.isScheduled(msg));
	}

	@Override
	public void accept(ZonedDateTime now) {
		this.msgScheduler.forEach(ms -> ms.handle(now));
	}
}
