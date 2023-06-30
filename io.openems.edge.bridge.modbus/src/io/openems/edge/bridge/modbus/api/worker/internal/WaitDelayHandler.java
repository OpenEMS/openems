package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.Collection;
import java.util.function.Consumer;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.EvictingQueue;

import io.openems.edge.bridge.modbus.api.task.WaitTask;

public class WaitDelayHandler {

	private static final int BUFFER_MS = 20;

	private final Runnable onWaitDelayTaskFinished;
	private final Consumer<Long> cycleDelayChannel;
	private final Stopwatch stopwatch;

	/**
	 * Delays that would have been possible. Updated via
	 * {@link #updateTotalNumberOfTasks(int)}.
	 */
	private EvictingQueue<Long> possibleDelays = EvictingQueue.create(10 /* initial size */);

	/**
	 * Set only via
	 * {@link #setWaitDelayTask(io.openems.edge.bridge.modbus.api.task.WaitTask.Delay)}.
	 */
	private WaitTask.Delay waitDelayTask;

	/**
	 * Marker for invalid time, e.g. CycleTasks span multiple Cycles, Tasks
	 * contained defective Components, etc.
	 */
	private boolean timeIsInvalid = false;

	protected WaitDelayHandler(Ticker ticker, Runnable onWaitDelayTaskFinished, Consumer<Long> cycleDelayChannel) {
		this.stopwatch = Stopwatch.createUnstarted(ticker);
		this.onWaitDelayTaskFinished = onWaitDelayTaskFinished;
		this.cycleDelayChannel = cycleDelayChannel;
		this.setWaitDelayTask(generateZeroWaitDelayTask(onWaitDelayTaskFinished));
	}

	protected WaitDelayHandler(Runnable onWaitDelayTaskFinished, Consumer<Long> cycleDelayChannel) {
		this(Ticker.systemTicker(), onWaitDelayTaskFinished, cycleDelayChannel);
	}

	/**
	 * Updates the size of the internal {@link #possibleDelays} queue to the total
	 * number of tasks.
	 * 
	 * <p>
	 * 'possibleDelays' needs to 'learn' execution time of all tasks, so it is
	 * important to keep its size in sync with the total number of tasks.
	 * 
	 * @param totalNumberOfTasks the total number of tasks
	 */
	public synchronized void updateTotalNumberOfTasks(int totalNumberOfTasks) {
		var targetQueueSize = Math.max(//
				totalNumberOfTasks * 5, // keeps 5 full Cycles
				10); // size at least 10

		// Calculate current queue size; logic is the inverse of
		// EvictingQueue#remainingCapacity()
		var currentQueueSize = this.possibleDelays.remainingCapacity() + this.possibleDelays.size();
		if (targetQueueSize != currentQueueSize) {
			// Size changed: create new queue and copy entries
			var oldQueue = this.possibleDelays;
			var newQueue = EvictingQueue.<Long>create(targetQueueSize);
			newQueue.addAll(oldQueue);
			this.possibleDelays = newQueue;
		}
	}

	/**
	 * Called on BEFORE_PROCESS_IMAGE event.
	 * 
	 * @param traceLog activate Trace-Log
	 * @return if traceLog is active, return a detailed log info; empty string
	 *         otherwise
	 */
	public synchronized String onBeforeProcessImage(boolean traceLog) {
		String log = "";

		if (this.timeIsInvalid) {
			// Do not add possibleDelay if previous Cycle contained a defective component
			if (traceLog) {
				log = "time is invalid";
			}
			this.stopwatch.reset();

		} else {
			// Calculate possible delay
			final long possibleDelay;
			if (this.stopwatch.isRunning()) {
				// Coming from FINISHED state -> it's possible to increase delay
				this.stopwatch.stop();
				possibleDelay = this.waitDelayTask.initialDelay + this.stopwatch.elapsed().toMillis();
				if (traceLog) {
					log = "measured possible delay [" + this.waitDelayTask.initialDelay + " + "
							+ this.stopwatch.elapsed().toMillis() + " = " + possibleDelay + "]";
				}

			} else {
				// FINISHED state has not happened -> reduce possible delay
				var halfOfLastDelay = this.waitDelayTask.initialDelay / 2;
				if (traceLog) {
					log = "FINISHED state has not happened -> reduce possible delay to [" + halfOfLastDelay + "]";
				}
				possibleDelay = halfOfLastDelay;
			}

			this.possibleDelays.add(possibleDelay);
		}

		// Initialize a new WaitDelayTask.
		this.setWaitDelayTask(generateWaitDelayTask(this.possibleDelays, this.onWaitDelayTaskFinished));

		// Reset 'timeIsInvalid'
		this.timeIsInvalid = false;

		return log;
	}

	/**
	 * Announce, that the Cycle measurement time is invalid.
	 * 
	 * <ul>
	 * <li>WaitDelayTask will be set to 'zero-wait'
	 * <li>Internal marker 'timeIsInvalid' is set. This causes the time measurement
	 * for this Cycle to be ignored
	 * </ul>
	 * 
	 * <p>
	 * This method is called shortly after 'onBeforeProcessImage()'
	 */
	public synchronized void timeIsInvalid() {
		this.setWaitDelayTask(generateZeroWaitDelayTask(this.onWaitDelayTaskFinished));
		this.timeIsInvalid = true;
	}

	/**
	 * Called when waiting finished.
	 */
	public synchronized void onFinished() {
		// Measure duration between FINISHED and ON_BEFORE_PROCESS_IMAGE event
		this.stopwatch.reset();
		this.stopwatch.start();
	}

	private synchronized void setWaitDelayTask(WaitTask.Delay waitDelayTask) {
		this.waitDelayTask = waitDelayTask;

		// Set the CYCLE_DELAY Channel
		this.cycleDelayChannel.accept(this.waitDelayTask.initialDelay);
	}

	/**
	 * Gets the {@link WaitTask.Delay}.
	 * 
	 * @return the task
	 */
	public synchronized WaitTask.Delay getWaitDelayTask() {
		return this.waitDelayTask;
	}

	/**
	 * Generates a {@link WaitDelayTask} with the minimum of all possible waiting
	 * times in the queue - minus {@link #BUFFER_MS}.
	 * 
	 * @param possibleDelays          the collected possible delays of the last
	 *                                Cycles
	 * @param onWaitDelayTaskFinished callback on wait-delay finished
	 * @return the {@link WaitDelayTask}
	 */
	protected static WaitTask.Delay generateWaitDelayTask(Collection<Long> possibleDelays,
			Runnable onWaitDelayTaskFinished) {
		var shortestPossibleDelay = (long) possibleDelays.stream() //
				.min(Long::compare) //
				.orElse(0L);

		if (shortestPossibleDelay < BUFFER_MS) {
			return generateZeroWaitDelayTask(onWaitDelayTaskFinished);
		}

		return new WaitTask.Delay(shortestPossibleDelay - BUFFER_MS, onWaitDelayTaskFinished);
	}

	/**
	 * Generates a {@link WaitDelayTask} with zero waiting time.
	 * 
	 * @param onWaitDelayTaskFinished callback on wait-delay finished
	 * @return the {@link WaitDelayTask}
	 */
	private static WaitTask.Delay generateZeroWaitDelayTask(Runnable onWaitDelayTaskFinished) {
		return new WaitTask.Delay(0, onWaitDelayTaskFinished);
	}
}
