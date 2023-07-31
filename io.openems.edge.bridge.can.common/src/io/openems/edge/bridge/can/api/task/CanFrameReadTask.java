package io.openems.edge.bridge.can.api.task;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.CanException;
import io.openems.edge.bridge.can.api.data.CanFrame;
import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.element.AbstractCanChannelElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Responsible Task for handling exactly one {@link CanFrame} identified by the
 * CAN address<br/>
 * and also all Channel Elements which are bind to this {@link CanFrame}<br/>
 * <br/>
 *
 * <p>
 * The 'CanFrameReadTask' will get all CAN frames with the specified CAN
 * address.<br/>
 * It will update all data within the local CAN Frame and appropriately notify
 * all Channel elements about a data change. <br/>
 * Note: The received CAN frames are collected by the {@link CanWorker} which
 * deliveres the frames to this instance.
 */
public class CanFrameReadTask extends AbstractTask implements ReadTask {

	private final Priority priority;

	private final List<CanRxTxData> recvdFrames = new ArrayList<>();
	// Measures the Cycle-Length between two consecutive _execute calls
	private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();

	private long timewindowBeforeChannelInvalidation; // ms

	private long lastCycleTime;
	private boolean suppressChannelDeactivationOnTimeout = false;

	@SafeVarargs
	public CanFrameReadTask(BridgeCan bridge, boolean isExtendedCanFrame, int canFrameAddress,
			int expectedCycleTimeInMs, Priority priority, AbstractCanChannelElement<?, ?>... elements) {
		super(bridge, isExtendedCanFrame, canFrameAddress, expectedCycleTimeInMs, elements);
		this.priority = priority;
		this.resetTimeWindowBeforeChannelInvalidation();

		// default value for first run
		this.lastCycleTime = this.getCanExpectedCycleTime();
	}

	@Override
	protected String getDescription() {
		return "CanFrameReadTask";
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	/**
	 * Suppresses channel deactivation on timeout.
	 */
	public void suppressChannelDeactivationOnTimeout() {
		this.suppressChannelDeactivationOnTimeout = true;
	}

	/*
	 * cle 2021.10.28 info for performance issues CanWorker calls this approx every
	 * core.cycleTime ms and every time it has fetched a frame
	 */

	@Override
	public synchronized void onReceivedFrame(CanRxTxData rxData) {
		this.recvdFrames.add(rxData);
		var size = this.recvdFrames.size();
		if (size > 15) {
			// NOTE: it may help to adjust the schedulers cycle time here
			this.logEvt.warn("CanFrameReadTask queue full: size " + this.recvdFrames.size() + " last adr "
					+ rxData.getAddress());
			// simply throw away old CAN frames
			this.recvdFrames.clear();
		}
	}

	private void resetTimeWindowBeforeChannelInvalidation() {
		var expectedCycleTime = this.getCanExpectedCycleTime();
		var maxErrorsBeforeInvalidation = this.bridge.invalidateElementsAfterReadErrors();
		this.timewindowBeforeChannelInvalidation = maxErrorsBeforeInvalidation * expectedCycleTime + 1;
	}

	private boolean checkCycleTimer() {
		this.timewindowBeforeChannelInvalidation -= this.lastCycleTime;
		if (this.timewindowBeforeChannelInvalidation < 0) {
			return false;
		}
		return true;
	}

	/*
	 * cle 2021.10.28 info for performance issues CanFrameReceiveTask calls this
	 * method Note: that we may have a hundred CanReadTasks
	 */
	private synchronized void evaluateCanFrame(AbstractCanBridge bridge) throws CanException {
		if (this.recvdFrames.size() == 0) {
			if (!this.checkCycleTimer()) {
				if (!this.suppressChannelDeactivationOnTimeout) {
					if (this.invalidateElements(bridge)) {
						throw new CanException("timeout exceeded.");
					}
				}
			}
			return;
		}
		this.resetTimeWindowBeforeChannelInvalidation();

		// cle 2021.10.28 we may have received more frames of the same time
		// this helps keeping the queue fill state low
		while (this.recvdFrames.size() != 0) {
			var rawFrame = this.recvdFrames.remove(0);
			this.canFrame.onUpdateData(rawFrame);
		}
	}

	@Override
	public int _execute(AbstractCanBridge bridge) throws OpenemsException {
		this.debugLog("_execute");

		// Measure the actual cycle-time; and starts the next measure cycle
		if (this.cycleStopwatch.isRunning()) {
			this.lastCycleTime = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
		this.cycleStopwatch.reset();
		this.cycleStopwatch.start();

		try {
			this.evaluateCanFrame(bridge);
		} catch (CanException e) {
			this.invalidateElements(bridge);
			throw new OpenemsException(e);
		}
		return 1;
	}

	private void debugLog(String txt) {
		if (this.getLogVerbosity() == LogVerbosity.ALL) {
			if (this.recvdFrames.size() > 0) {
				this.logCan.info("CanFrameReadTask " + txt + " CAN adr: " + this.getCanAddress() + " expcycle "
						+ this.getCanExpectedCycleTime() + "ms RxQueue: " + this.recvdFrames.size());
			}
		}
	}

}
