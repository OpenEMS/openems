package io.openems.edge.bridge.modbus.api.worker;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import com.google.common.collect.EvictingQueue;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.task.ReadTask;
import io.openems.edge.bridge.modbus.api.task.WaitTask;

/**
 * WaitHandler is responsible for calculating a 'wait' time before execution of
 * Modbus {@link ReadTask}s in {@link ModbusWorker}. The target is to read data
 * from a Modbus device 'as late as possible' before ON_BEFORE_PROCESS_IMAGE
 * event. It uses an internal queue of waiting times that would have been
 * possible in previous Cycles. The minimum value of this queue is taken as next
 * waiting time.
 */
public class WaitHandler {

	private static final int BUFFER_MS = 20;

	public final AtomicReference<WaitTask> activeWaitTask = new AtomicReference<>();

	private final AtomicReference<LogVerbosity> logVerbosity;

	/**
	 * Waiting times that would have been possible.
	 */
	private EvictingQueue<Long> possibleWaitingTimes = EvictingQueue.create(1);

	private Instant stopwatch = null;
	private long lastWaitingTime = 0;
	private boolean isCycleTimeTooShort = false;
	private boolean isCycleContainedDefectiveComponent = false;

	public WaitHandler(AtomicReference<LogVerbosity> logVerbosity) {
		this.logVerbosity = logVerbosity;
	}

	/**
	 * Update the size of the internal 'possibleWaitingTimes' queue.
	 * 
	 * <p>
	 * Size is taken from the total number of LOW priority tasks. As there is one
	 * LOW priority task executed per Cycle, this size is the total number of Cycles
	 * that are required to have each task executed once.
	 * 
	 * @param size new size
	 */
	public synchronized void updateSize(int size) {
		if (size == this.possibleWaitingTimes.size()) {
			return;
		}
		EvictingQueue<Long> delays = EvictingQueue.create(size);
		delays.addAll(this.possibleWaitingTimes);
		this.possibleWaitingTimes = delays;
	}

	/**
	 * Receive TOPIC_CYCLE_BEFORE_PROCESS_IMAGE event.
	 */
	public void onBeforeProcessImage() {
		var now = Instant.now();

		final long possibleWaitingTime;
		final boolean isCycleTimeTooShort;
		if (this.stopwatch == null) {
			// No pending Tasks did never happen -> Tasks could not be finished
			this.log("'AllTasksFinished' did never happen"); // TODO remove before merge
			possibleWaitingTime = 0L;
			isCycleTimeTooShort = true;
		} else {
			// Calculate possible waiting time
			possibleWaitingTime = this.lastWaitingTime + Duration.between(this.stopwatch, now).toMillis();
			isCycleTimeTooShort = false;
			this.stopwatch = null;
		}

		this.isCycleTimeTooShort = isCycleTimeTooShort;
		if (this.isCycleContainedDefectiveComponent) {
			// Do not add possibleWaitingTime if this Cycle contained a defective component.
			// Additionally remove recent zeros, because there might have been an overlap of
			// Tasks and Cycles, i.e. reading from a defective component might have already
			// blocked a previous Cycle and caused a zero possible waiting time.
			var hasNonZeroElements = this.possibleWaitingTimes.stream().anyMatch(v -> v > 0);
			if (hasNonZeroElements) {
				Iterator<Long> i = this.possibleWaitingTimes.iterator();
				var foundNonZeroElement = false;
				while (i.hasNext()) {
					var v = i.next();
					if (foundNonZeroElement && v == 0) {
						i.remove();
					} else if (!foundNonZeroElement && v > 0) {
						foundNonZeroElement = true;
					}
				}
			}
		} else {
			this.possibleWaitingTimes.add(possibleWaitingTime);
		}
		this.isCycleContainedDefectiveComponent = false;
	}

	/**
	 * Gets the {@link WaitTask} with the minimum of all possible waiting times in
	 * the queue; or null if possible waiting time is zero.
	 * 
	 * @return the {@link WaitTask} or null
	 */
	public WaitTask getWaitTask() {
		var waitingTime = this.possibleWaitingTimes.stream() //
				.min(Long::compare) //
				.orElseGet(() -> 0L);
		this.log("WaitingTimes [" + this.possibleWaitingTimes.size() + "] " + this.possibleWaitingTimes + " -> "
				+ waitingTime); // TODO remove before merge
		if (waitingTime == null) {
			this.lastWaitingTime = 0;
			return null;
		}

		waitingTime -= BUFFER_MS; // Reduce waiting time by BUFFER
		if (waitingTime <= 0) {
			this.lastWaitingTime = 0;
			return null;
		}
		this.lastWaitingTime = waitingTime;
		return new WaitTask(waitingTime);
	}

	/**
	 * No pending Tasks in Queue.
	 */
	public void onAllTasksFinished() {
		this.stopwatch = Instant.now();
	}

	/**
	 * Is the Cycle-Time too short?.
	 * 
	 * @return true if it is too short
	 */
	public boolean isCycleTimeTooShort() {
		return this.isCycleTimeTooShort;
	}

	/**
	 * Announces that the current cycle contained a defective Component. This causes
	 * the current possible waiting time to be ignored.
	 */
	public void setCycleContainedDefectiveComponent() {
		this.isCycleContainedDefectiveComponent = true;
	}

	// TODO remove before release
	private void log(String message) {
		switch (this.logVerbosity.get()) {
		case DEV_REFACTORING:
			System.out.println("WaitHandler: " + message);
			break;
		case NONE:
		case READS_AND_WRITES:
		case WRITES:
			break;
		}
	}

}
