package io.openems.edge.bridge.modbus.api.worker.internal;

import com.google.common.base.Stopwatch;

public class WaitDelayHandler {

	private final Runnable onWaitDelayTaskFinished;
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();

	private WaitDelayTask waitDelayTask;

	public WaitDelayHandler(Runnable onWaitDelayTaskFinished) {
		this.onWaitDelayTaskFinished = onWaitDelayTaskFinished;
		this.waitDelayTask = new WaitDelayTask(0, this.onWaitDelayTaskFinished);
	}

	public void onBeforeProcessImage() {
		final long possibleDelay;
		if (this.stopwatch.isRunning()) {
			// Coming from FINISHED state -> it's possible to increase delay
			this.stopwatch.stop();
			possibleDelay = this.waitDelayTask.initialDelay + this.stopwatch.elapsed().toMillis();
			System.out.println("Possible delay: " + possibleDelay);
		} else {
			// FINISHED state has not happened -> reduce possible delay
			this.stopwatch.stop();
			possibleDelay = 0;
		}

		// TODO calculate next delay
		// TODO handle defective components; copy from old WaitHandler

		// Initialize a new WaitDelayTask.
		var delay = 100; // TODO
		this.waitDelayTask = new WaitDelayTask(delay, this.onWaitDelayTaskFinished);
	}

	public void onFinished() {
		// Measure duration between FINISHED and ON_BEFORE_PROCESS_IMAGE eventT
		this.stopwatch.start();
	}

	public WaitDelayTask getWaitDelayTask() {
		return this.waitDelayTask;
	}

}
