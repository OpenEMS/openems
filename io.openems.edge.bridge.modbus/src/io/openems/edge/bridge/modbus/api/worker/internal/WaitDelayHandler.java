package io.openems.edge.bridge.modbus.api.worker.internal;

import java.util.concurrent.atomic.AtomicReference;

import com.google.common.base.Stopwatch;
import com.google.common.base.Ticker;
import com.google.common.collect.EvictingQueue;

import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.task.WaitTask;

public class WaitDelayHandler {

	private static final int BUFFER_MS = 20;

	private final Runnable onWaitDelayTaskFinished;
	private final Stopwatch stopwatch;

	private final AtomicReference<LogVerbosity> logVerbosity;

	/** Delays that would have been possible. */
	private final EvictingQueue<Long> possibleDelays = EvictingQueue.create(10 /* TODO size? */);

	private WaitDelayTask waitDelayTask;

	private boolean previousCycleContainedDefectiveComponents;

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
	 * 
	 * @param nextCycleContainsDefectiveComponents true if the {@link CycleTasks}
	 *                                             contain tasks of a defective
	 *                                             component
	 */
	public void onBeforeProcessImage(boolean nextCycleContainsDefectiveComponents) {
		if (this.previousCycleContainedDefectiveComponents) {
			// Do not add possibleDelay if previous Cycle contained a defective component
			this.log("onBeforeProcessImage: previous Cycle contained a defective component");
			this.stopwatch.reset();

		} else {
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
		if (nextCycleContainsDefectiveComponents) {
			this.waitDelayTask = this.generateZeroWaitDelayTask();
		} else {
			this.waitDelayTask = this.generateWaitDelayTask();
		}

		this.previousCycleContainedDefectiveComponents = nextCycleContainsDefectiveComponents;
	}

	/**
	 * Called when waiting finished.
	 */
	public void onFinished() {
		// Measure duration between FINISHED and ON_BEFORE_PROCESS_IMAGE eventT
		this.stopwatch.start();
	}

	/**
	 * Gets the {@link WaitDelayTask}.
	 * 
	 * @return the task
	 */
	public WaitDelayTask getWaitDelayTask() {
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
			System.out.println("WaitDelayHandler: " + message);
			break;
		case NONE:
		case READS_AND_WRITES:
		case WRITES:
			break;
		}
	}
}
