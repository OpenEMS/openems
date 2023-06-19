package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.EvictingQueue;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.task.WaitTask;

public class WaitDelayHandler {

	private static final int BUFFER_MS = 20;

	private final Logger log = LoggerFactory.getLogger(WaitDelayHandler.class);

	private final Runnable onWaitDelayTaskFinished;
	private final Stopwatch stopwatch;

	private final AtomicReference<LogVerbosity> logVerbosity;

	/** Delays that would have been possible. */
	private final EvictingQueue<Long> possibleDelays = EvictingQueue.create(10 /* TODO size? */);

	private WaitDelayTask waitDelayTask;

	/**
	 * Marker for invalid time, e.g. CycleTasks span multiple Cycles, Tasks
	 * contained defective Components, etc.
	 */
	private boolean timeIsInvalid = false;

	protected WaitDelayHandler(Ticker ticker, AtomicReference<LogVerbosity> logVerbosity,
			Runnable onWaitDelayTaskFinished) {
		this.logVerbosity = logVerbosity;
		this.stopwatch = Stopwatch.createUnstarted(ticker);
		this.onWaitDelayTaskFinished = onWaitDelayTaskFinished;
		this.waitDelayTask = this.generateZeroWaitDelayTask();
	}

	protected WaitDelayHandler(Ticker ticker, Runnable onWaitDelayTaskFinished) {
		this(Ticker.systemTicker(), new AtomicReference<>(LogVerbosity.NONE), onWaitDelayTaskFinished);
	}

	protected WaitDelayHandler(AtomicReference<LogVerbosity> logVerbosity, Runnable onWaitDelayTaskFinished) {
		this(Ticker.systemTicker(), logVerbosity, onWaitDelayTaskFinished);
	}

	/**
	 * Called on BEFORE_PROCESS_IMAGE event.
	 */
	public synchronized void onBeforeProcessImage() {
		if (this.timeIsInvalid) {
			// Do not add possibleDelay if previous Cycle contained a defective component
			this.log("onBeforeProcessImage: time is invalid");
			this.stopwatch.reset();

		} else {
			// Calculate possible delay
			final long possibleDelay;
			if (this.stopwatch.isRunning()) {
				// Coming from FINISHED state -> it's possible to increase delay
				this.stopwatch.stop();
				possibleDelay = this.waitDelayTask.initialDelay + this.stopwatch.elapsed().toMillis();
				this.log("onBeforeProcessImage: measured possible delay [" + this.waitDelayTask.initialDelay + " + "
						+ this.stopwatch.elapsed().toMillis() + " = " + possibleDelay + "]");

			} else {
				// FINISHED state has not happened -> reduce possible delay
				this.log("onBeforeProcessImage: FINISHED state has not happened -> reduce possible delay");
				possibleDelay = 0;
			}

			this.possibleDelays.add(possibleDelay);
		}

		// Initialize a new WaitDelayTask.
		this.waitDelayTask = this.generateWaitDelayTask();

		// Reset 'timeIsInvalid'
		this.timeIsInvalid = false;
	}

	/**
	 * Announce, that the Cycle measurement time is invalid.
	 * 
	 * <ul>
	 * <li>WaitDelayTask will be set to 'zero-wait'
	 * <li>Internal marker 'previousCycleContainedDefectiveComponents' is set. This
	 * causes the time measurement for this Cycle to be ignored
	 * </ul>
	 * 
	 * <p>
	 * This method is called shortly after 'onBeforeProcessImage()'
	 */
	public synchronized void timeIsInvalid() {
		this.waitDelayTask = this.generateZeroWaitDelayTask();
		this.timeIsInvalid = true;
	}

	/**
	 * Called when waiting finished.
	 */
	public synchronized void onFinished() {
		this.log("onFinished");
		// Measure duration between FINISHED and ON_BEFORE_PROCESS_IMAGE event
		this.stopwatch.reset();
		this.stopwatch.start();
	}

	/**
	 * Gets the {@link WaitDelayTask}.
	 * 
	 * @return the task
	 */
	public synchronized WaitDelayTask getWaitDelayTask() {
		return this.waitDelayTask;
	}

	/**
	 * Gets the {@link WaitTask} with the minimum of all possible waiting times in
	 * the queue; or null if possible waiting time is zero.
	 * 
	 * @return the {@link WaitTask} or null
	 */
	private WaitDelayTask generateWaitDelayTask() {
		var shortestPossibleDelay = this.possibleDelays.stream() //
				.min(Long::compare) //
				.orElseGet(() -> 0L);
		this.log("generateWaitDelayTask: PossibleDelays " //
				+ "[" + this.possibleDelays.size() + "/"
				+ (this.possibleDelays.size() + this.possibleDelays.remainingCapacity()) + "] " //
				+ this.possibleDelays + " -> " + shortestPossibleDelay);
		final long delay;
		if (shortestPossibleDelay == null || shortestPossibleDelay < BUFFER_MS) {
			delay = 0;
		} else {
			delay = shortestPossibleDelay - BUFFER_MS;
		}
		return new WaitDelayTask(delay, this.onWaitDelayTaskFinished);
	}

	private WaitDelayTask generateZeroWaitDelayTask() {
		this.log("generateZeroWaitDelayTask");
		return new WaitDelayTask(0, this.onWaitDelayTaskFinished);
	}

	// TODO remove before release
	private void log(String message) {
		switch (this.logVerbosity.get()) {
		case DEV_REFACTORING:
			this.log.info(message);
			break;
		case NONE:
		case READS_AND_WRITES:
		case WRITES:
			break;
		}
	}
}
