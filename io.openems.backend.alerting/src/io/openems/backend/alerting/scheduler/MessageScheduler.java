package io.openems.backend.alerting.scheduler;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.PriorityQueue;

import io.openems.backend.alerting.Handler;
import io.openems.backend.alerting.Message;

public class MessageScheduler<T extends Message> {

	private final Map<String, T> messageForId;
	private final PriorityQueue<T> queue;
	private final Handler<T> handler;

	public MessageScheduler(Handler<T> handler) {
		this.handler = handler;
		this.queue = new PriorityQueue<>();
		this.messageForId = new HashMap<>();
	}

	/**
	 * Add message to scheduler.
	 *
	 * @param msg to add
	 */
	public void schedule(T msg) {
		this.queue.add(msg);
		this.messageForId.put(msg.getId(), msg);
	}

	/**
	 * Remove message from scheduler.
	 *
	 * @param msgId for message to remove
	 */
	public void remove(String msgId) {
		if (msgId == null) {
			return;
		}
		var msg = this.messageForId.get(msgId);
		this.queue.remove(msg);
		this.messageForId.remove(msgId);
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
	 * Get amount of scheduled messages.
	 *
	 * @return size of message queue
	 */
	public int size() {
		return this.queue.size();
	}

	/**
	 * Transfer the messages due to their handler.
	 */
	public void handle() {
		var now = ZonedDateTime.now();
		var msgs = new ArrayList<T>();
		while (!this.queue.isEmpty() && now.isAfter(this.queue.peek().getNotifyStamp())) {
			msgs.add(this.queue.poll());
		}
		if (!msgs.isEmpty()) {
			this.handler.send(now, msgs);
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
