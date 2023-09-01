package io.openems.backend.alerting.scheduler;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes subscriber every full Minute. Starts and stops itself, depending on
 * whether subscribers are present.
 */
public class MinuteTimer {
	
	private final Logger log = LoggerFactory.getLogger(MinuteTimer.class);
	private final List<Consumer<ZonedDateTime>> subscriber  = new ArrayList<>();
	private final Clock clock;
	
	public MinuteTimer(Clock clock) {
		this.clock = clock;
	}
	
	/**
	 * Create a custom MinuteTimer with given clock.
	 * 
	 * @param clock to use for timing
	 * @return a minute accurate timer
	 */
	public MinuteTimer custom(Clock clock) {
		return new MinuteTimer(clock);
	}

	/**
	 * Add subscriber for every minute execution.
	 *
	 * @param sub to add
	 */
	public void subscribe(Consumer<ZonedDateTime> sub) {
		this.subscriber.add(sub);
	}

	/**
	 * Remove subscriber from every minute execution.
	 *
	 * @param sub to remove
	 */
	public void unsubscribe(Consumer<ZonedDateTime> sub) {
		if (sub == null) {
			return;
		}
		this.subscriber.remove(sub);
		if (this.subscriber.isEmpty()) {
			this.stop();
		}
	}

	protected void start() {
		this.log.info("[Alerting-MinuteTimer] start");
	}

	protected void cycle() {
		this.log.debug("[Alerting-MinuteTimer] cycle");
		var now = ZonedDateTime.now(this.clock);
		this.subscriber.forEach((sub) -> {
			try {
				sub.accept(now);
			} catch (Throwable t) {
				this.log.error(t.getMessage(), t);
			}
		});
	}

	protected void stop() {
		this.log.info("[Alerting-MinuteTimer] stop");
	}
	
	public int getSubscriberCount() {
		return this.subscriber.size();
	}
}
