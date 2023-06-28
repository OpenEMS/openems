package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.Collection;
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

	private WaitTask.Delay waitDelayTask;

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
		this.waitDelayTask = generateZeroWaitDelayTask(onWaitDelayTaskFinished);
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
		final String log;

		if (this.timeIsInvalid) {
			// Do not add possibleDelay if previous Cycle contained a defective component
			log = "time is invalid";
			this.stopwatch.reset();

		} else {
			// Calculate possible delay
			final long possibleDelay;
			if (this.stopwatch.isRunning()) {
				// Coming from FINISHED state -> it's possible to increase delay
				this.stopwatch.stop();
				possibleDelay = this.waitDelayTask.initialDelay + this.stopwatch.elapsed().toMillis();
				log = "measured possible delay [" + this.waitDelayTask.initialDelay + " + "
						+ this.stopwatch.elapsed().toMillis() + " = " + possibleDelay + "]";

			} else {
				// FINISHED state has not happened -> reduce possible delay
				log = "FINISHED state has not happened -> reduce possible delay to zero";
				possibleDelay = 0;
			}

			this.possibleDelays.add(possibleDelay);
		}

		// Initialize a new WaitDelayTask.
		this.waitDelayTask = generateWaitDelayTask(this.possibleDelays, this.onWaitDelayTaskFinished);

		// Reset 'timeIsInvalid'
		this.timeIsInvalid = false;

		this.log("onBeforeProcessImage: " + log + "; delay=" + this.waitDelayTask.initialDelay);
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
		this.waitDelayTask = generateZeroWaitDelayTask(this.onWaitDelayTaskFinished);
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
