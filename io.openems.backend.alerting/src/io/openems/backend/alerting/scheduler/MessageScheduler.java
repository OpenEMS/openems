package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;
import java.util.function.Predicate;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;

/**
 * Schedules one or more {@link Message} for type {@link T} to a specific time.
 * <p>
 * After the specified time is reached, the scheduler sends the Messages to
 * their {@link Handler} and removes them from itself.
 * </p>
 * 
 * @author kai.jeschek
 *
 * @param <T> type of Message
 */
public class MessageScheduler<T extends Message> {
	private final Map<String, T> messageForId;
	private final PriorityQueue<T> queue;

	private final Handler<T> handler;

	public MessageScheduler(Handler<T> handler) {
		this.handler = handler;
		this.queue = new PriorityQueue<>();
		this.messageForId = new TreeMap<>();
	}

	/**
	 * Add message to scheduler.
	 *
	 * @param msg to add
	 */
	public void schedule(T msg) {
		if (msg != null) {
			synchronized (this) {
				this.messageForId.computeIfAbsent(msg.getId(), (key) -> {
					this.queue.add(msg);
					return msg;
				});
			}
		}
	}

	/**
	 * Remove message from scheduler.
	 *
	 * @param msgId for message to remove
	 */
	public void remove(String msgId) {
		if (msgId != null) {
			synchronized (this) {
				var msg = this.messageForId.remove(msgId);
				if (msg != null) {
					this.queue.remove(msg);
				}
			}
		}
	}

	/**
	 * Get if given message is scheduled.
	 *
	 * @param msg to check for
	 * @return true if is scheduled
	 */
	public boolean isScheduled(Message msg) {
		return this.queue.contains(msg);
	}

	/**
	 * Get if a message fitting the {@link Predicate} is scheduled.
	 *
	 * @param find ;filter to use
	 * @return true if is scheduled
	 */
	public boolean isScheduled(Predicate<T> find) {
		return this.queue.stream().anyMatch(find);
	}

	public Class<T> getGeneric() {
		return this.handler.getGeneric();
	}

	/**
	 * Get amount of scheduled messages.
	 *
	 * @return size of message queue
	 */
	public int size() {
		return this.queue.size();
	}

	/**
	 * Transfer the messages due to their handler.
	 * 
	 * @param now TimeStamp on call
	 */
	public void handle(ZonedDateTime now) {
		var msgs = new ArrayList<T>();
		while (this.hasTimeElapsed(now, this.queue.peek())) {
			msgs.add(this.poll());
		}
		if (!msgs.isEmpty()) {
			this.handler.send(now, msgs);
		}
	}

	private boolean hasTimeElapsed(ZonedDateTime now, T msg) {
		return msg != null && now.isAfter(msg.getNotifyStamp());
	}

	private T poll() {
		synchronized (this) {
			var msg = this.queue.poll();
			if (msg != null) {
				this.messageForId.remove(msg.getId());
			}
			return msg;
		}
	}

	/**
	 * Check if this MessageScheduler handles messages for given handler.
	 *
	 * @param handler to check for
	 * @return true if this MessageScheduler handles messages for given handler
	 */
	public boolean isFor(Handler<?> handler) {
		return this.handler == handler;
	}
}
