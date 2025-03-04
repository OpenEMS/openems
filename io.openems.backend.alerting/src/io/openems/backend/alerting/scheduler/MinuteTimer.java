package io.openems.backend.alerting.scheduler;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes subscriber every full Minute or once after a specified time of
 * Minutes. Starts and stops itself, depending on whether subscribers are
 * present.
 *
 */
public class MinuteTimer implements TimedExecutor {

	private final Logger log = LoggerFactory.getLogger(MinuteTimer.class);

	private final List<Consumer<ZonedDateTime>> subscriber = new ArrayList<>();
	private final PriorityBlockingQueue<TimedTask> singleTasks = new PriorityBlockingQueue<>();

	private final Clock clock;
	private long cycleCount = 0;

	private boolean isRunning = false;

	/**
	 * Create MinuteTimer with given clock.
	 *
	 * @param clock to use for timing
	 */
	public MinuteTimer(Clock clock) {
		this.clock = clock;
	}

	/**
	 * Add subscriber for every minute execution.
	 *
	 * @param sub to add
	 */
	public void subscribe(Consumer<ZonedDateTime> sub) {
		this.subscriber.add(sub);
		if (this.subscriber.size() > 0) {
			this.start();
		}
	}

	/**
	 * Remove subscriber from every minute execution.
	 *
	 * @param sub to remove
	 */
	public void unsubscribe(Consumer<ZonedDateTime> sub) {
		if (sub != null) {
			this.subscriber.remove(sub);
		}
		if (this.subscriber.size() == 0) {
			this.stop();
		}
	}

	/**
	 * Execute the given task at the given dateTime to the minute.
	 *
	 * @param at   time to execute at
	 * @param task task to execute
	 *
	 * @return reference to Task. Can be used to cancel the task.
	 */
	@Override
	public TimedTask schedule(ZonedDateTime at, Consumer<ZonedDateTime> task) {
		final var singleTask = new TimedTask(at, task);
		this.singleTasks.add(singleTask);
		return singleTask;
	}

	/**
	 * Cancel task.
	 *
	 * @param task to remove
	 */
	@Override
	public void cancel(TimedTask task) {
		if (task != null) {
			this.singleTasks.remove(task);
		}
	}

	private synchronized boolean empty() {
		return this.subscriber.isEmpty() && this.singleTasks.isEmpty();
	}

	protected synchronized void start() {
		if (this.isRunning) {
			return;
		}
		this.log.debug("START");
		this.isRunning = true;
	}

	private void logDebugInfos() {
		if (this.cycleCount % 5 == 0) {
			this.log.debug("MinuteTimer[ Now:%s, Ticks:%s ]".formatted(this.now(), this.cycleCount));
		}
	}

	protected synchronized void cycle() {
		if (!this.isRunning) {
			return;
		}

		this.cycleCount++;
		this.logDebugInfos();

		final var now = this.now();

		this.callSubscriber(now);
		this.callSingleTasks(now);

		if (this.empty()) {
			this.stop();
		}
	}

	private void callSubscriber(ZonedDateTime now) {
		for (var sub : this.subscriber) {
			try {
				sub.accept(now);
			} catch (Throwable t) {
				this.log.error(t.getMessage(), t);
			}
		}
	}

	private void callSingleTasks(ZonedDateTime now) {
		while (!this.singleTasks.isEmpty() //
				&& this.singleTasks.peek().executeAt.isBefore(now)) {
			try {
				this.singleTasks.poll().task.accept(now);
			} catch (Throwable t) {
				this.log.error(t.getMessage(), t);
			}
		}
	}

	protected synchronized void stop() {
		if (!this.isRunning) {
			return;
		}
		this.log.debug("STOP");
		this.isRunning = false;
	}

	public int getSubscriberCount() {
		return this.subscriber.size();
	}

	/**
	 * Get the current {@link ZonedDateTime} for the {@link Clock} this
	 * {@link MinuteTimer} works with.
	 *
	 * @return {@link ZonedDateTime} now
	 */
	@Override
	public ZonedDateTime now() {
		return ZonedDateTime.now(this.clock);
	}
}
