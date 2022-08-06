package io.openems.backend.alerting;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.EdgeUser;
import io.openems.common.utils.ThreadPoolUtils;

public class ScheduleMessageService {

	private final Logger log = LoggerFactory.getLogger(ScheduleMessageService.class);
	private final Consumer<Message> action;

	private ScheduledExecutorService scheduler;

	/**
	 * Key = EdgeID, Task = List of all users to notify.
	 */
	private final TreeMap<String, MessageTask> mapEdgeTasks;

	private final Alerting parent;

	public ScheduleMessageService(Alerting parent, Consumer<Message> action) {
		this.parent = parent;

		this.action = action;
		this.mapEdgeTasks = new TreeMap<>();
	}

	/**
	 * Start the scheduler and reserve Threads.
	 */
	public void start() {
		this.scheduler = Executors.newScheduledThreadPool(3);
	}

	/**
	 * Stop all Tasks and free Threads.
	 */
	public void stop() {
		final int taskCnt = this.mapEdgeTasks.size();

		Set<String> edges = this.mapEdgeTasks.keySet();
		edges.forEach(e -> {
			this.mapEdgeTasks.get(e).cancel();
		});
		ThreadPoolUtils.shutdownAndAwaitTermination(this.scheduler, 10);
		this.scheduler = null;

		if (taskCnt > 0) {
			this.logDebug("Stopped " + taskCnt + " Tasks");
		}
	}

	/**
	 * Add users to Tasks list.
	 *
	 * @param edgeUsers users to add
	 * @param edgeId    involved Edge
	 */
	public void createTask(Map<ZonedDateTime, List<EdgeUser>> edgeUsers, String edgeId) {
		if (this.mapEdgeTasks.containsKey(edgeId)) {
			this.logDebug("Invalid task for already scheduled Edge [" + edgeId + ']');
			return;
		}

		MessageTask edgeTasks = new MessageTask(edgeId);
		edgeUsers.forEach((timeStamp, listUsers) -> {
			Message message = new Message(timeStamp, listUsers, edgeId);

			if (!timeStamp.isAfter(ZonedDateTime.now())) {
				this.action.accept(message);
			} else {
				edgeTasks.add(message);
			}
		});

		if (!edgeTasks.isEmpty()) {
			edgeTasks.start();

			final AtomicReference<Integer> userCnt = new AtomicReference<>(0);
			edgeTasks.forEach(message -> {
				userCnt.set(userCnt.get() + message.getUser().size());
			});
			this.logDebug("Add task for [" + edgeId + "] | No of Users: " + userCnt.get());
		}
	}

	private void schedule(MessageTask task, ZonedDateTime timeStamp) {
		long msec = Math.max(ChronoUnit.MILLIS.between(LocalDateTime.now(), timeStamp), 0);
		this.scheduler.schedule(task, msec, TimeUnit.MILLISECONDS);
	}

	/**
	 * Remove Task associated with edge.
	 *
	 * @param edgeID of Edge
	 */
	public void removeAll(String edgeID) {
		MessageTask edgeTask = this.mapEdgeTasks.get(edgeID);
		if (edgeTask == null) {
			return;
		}

		this.logInfo("Remove all tasks for Edge [" + edgeID + "]");
		edgeTask.cancel();
	}

	private void logInfo(String info) {
		if (this.parent != null) {
			this.parent.logInfo(this.log, info);
		}
	}

	private void logDebug(String debug) {
		if (this.parent != null) {
			this.parent.logDebug(this.log, debug);
		}
	}

	/**
	 * Amount of Tasks scheduled.
	 *
	 * @return size
	 */
	public int size() {
		AtomicInteger count = new AtomicInteger(0);
		this.mapEdgeTasks.forEach((edgeId, task) -> {
			count.addAndGet(task.size());
		});
		return count.get();
	}

	/**
	 * Returns true if no Tasks are scheduled.
	 *
	 * @return if empty
	 */
	public boolean isEmpty() {
		return this.mapEdgeTasks.isEmpty();
	}

	/**
	 * Returns true if a Task for the given edgeId is scheduled.
	 *
	 * @param edgeId EdgeID
	 * @return contains edge
	 */
	public boolean contains(String edgeId) {
		return this.mapEdgeTasks.containsKey(edgeId);
	}

	/**
	 * Remove all Tasks.
	 */
	public void clear() {
		this.mapEdgeTasks.forEach((edgeId, edgeTask) -> {
			this.logInfo("Remove all tasks for Edge [" + edgeId + "]");
			edgeTask.cancel();
		});
		this.mapEdgeTasks.clear();
	}

	private class MessageTask extends PriorityQueue<Message> implements Runnable {
		private static final long serialVersionUID = 7451066803623212826L;
		private final String edgeId;

		private MessageTask(String edgeId) {
			this.edgeId = edgeId;
		}

		@Override
		public void run() {
			if (super.isEmpty()) {
				return;
			}

			Message message = super.poll();
			ScheduleMessageService.this.action.accept(message);

			this.revive();
		}

		public void start() {
			ScheduleMessageService.this.mapEdgeTasks.put(this.edgeId, this);
			this.revive();
		}

		private void revive() {
			if (!this.isEmpty()) {
				ScheduleMessageService.this.schedule(this, super.peek().getNotifyStamp());
			} else {
				this.cancel();
			}
		}

		public boolean cancel() {
			super.clear();
			return ScheduleMessageService.this.mapEdgeTasks.remove(this.edgeId) == this;
		}
	}

}
