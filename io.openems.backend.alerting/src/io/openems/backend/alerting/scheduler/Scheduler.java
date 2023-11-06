package io.openems.backend.alerting.scheduler;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;

/**
 * A {@link MessageSchedulerService} implementation using {@link MinuteTimer}.
 *
 */
public class Scheduler implements Runnable, MessageSchedulerService {

	private final MinuteTimer minuteTimer;
	private final List<MessageScheduler<? extends Message>> msgScheduler;

	private final Logger log = LoggerFactory.getLogger(Scheduler.class);

	public Scheduler() {
		this.minuteTimer = MinuteTimer.getInstance();
		this.msgScheduler = new ArrayList<>();
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
	 * Check if given message is scheduled within any MessageScheduler.
	 *
	 * @param msg to check for
	 * @return true if any scheduling was found
	 */
	public boolean isScheduled(Message msg) {
		return this.msgScheduler.stream().anyMatch(ms -> ms.isScheduled(msg));
	}

	@Override
	public void run() {
		this.msgScheduler.forEach(MessageScheduler::handle);
	}
}
