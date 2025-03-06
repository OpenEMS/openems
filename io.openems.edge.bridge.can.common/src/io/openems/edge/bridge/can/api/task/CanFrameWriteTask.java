package io.openems.edge.bridge.can.api.task;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.LogVerbosity;
import io.openems.edge.bridge.can.api.BridgeCan;
import io.openems.edge.bridge.can.api.CanException;
import io.openems.edge.bridge.can.api.data.CanFrame;
import io.openems.edge.bridge.can.api.element.AbstractCanChannelElement;
import io.openems.edge.common.taskmanager.Priority;

/**
 * Responsible Task for handling exactly one {@link CanFrame} identified by the
 * CAN address<br/>
 * and also all Channel Elements which are bind to this {@link CanFrame} <br/>
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
public class CanFrameWriteTask extends AbstractTask implements WriteTask {

	private final Priority priority;

	/** Measures the Cycle-Length between two consecutive _execute calls. */
	private final Stopwatch cycleStopwatch = Stopwatch.createUnstarted();

	private final long frameSendCycleTime; // ms

	private long taskCycleTime; // ms

	private long timeSinceLastCanSend; // ms

	private boolean warnOnce = true;

	private boolean addFrameOnce = true;

	private AbstractCanChannelElement<?, ?> theElement = null;

	private final boolean useLowLevelCyclicalSendTask;

	@SafeVarargs
	public CanFrameWriteTask(BridgeCan bridge, boolean useLowLevelCyclicalSendTask, boolean isExtendedCanFrame,
			int canFrameAddress, int expectedCycleTimeInMs, Priority priority,
			AbstractCanChannelElement<?, ?>... elements) throws OpenemsException {
		super(bridge, isExtendedCanFrame, canFrameAddress, expectedCycleTimeInMs, elements);
		this.useLowLevelCyclicalSendTask = useLowLevelCyclicalSendTask;
		this.priority = priority;
		this.frameSendCycleTime = this.getCanExpectedCycleTime();
		this.taskCycleTime = this.frameSendCycleTime;
		this.timeSinceLastCanSend = 0;

		if (elements != null && elements.length == 1) {
			if (elements[0].hasOwnCanTemplateFormat()) {
				this.canFrame.setData(elements[0].getOwnCanTemplateData());
				this.theElement = elements[0];
			}
		}
	}

	private synchronized void sendCanFrame(AbstractCanBridge bridge) throws CanException {
		// check if frameSendCycleTime has passed
		this.timeSinceLastCanSend += this.taskCycleTime;
		if (this.timeSinceLastCanSend + this.taskCycleTime < this.frameSendCycleTime) {
			// we still can wait for another task cycle before frameSendCycleTime has passed
			return;
		}
		this.timeSinceLastCanSend = 0;

		if (this.warnOnce && this.taskCycleTime > this.frameSendCycleTime) {
			this.warnOnce = false;

			this.logEvt.warn("CanFrameWriteTask [CAN Adr:" + this.getCanAddress()
					+ "]: Schedulers cycle time is larger, than this CAN frames cycle time " + this.frameSendCycleTime
					+ " ms.");
		}

		try {
			if (this.useLowLevelCyclicalSendTask) {
				if (this.addFrameOnce) {
					this.addFrameOnce = false;
					bridge.getCanConnection().sendCyclicallyAdd(this.canFrame, (int) this.taskCycleTime);
				} else {
					bridge.getCanConnection().sendCyclicallyAdopt(this.canFrame);
				}
				this.canFrame.onCanFrameSuccessfullySend(); // simulate send feedback
			} else if (this.canFrame.isAllowedTx()) {

				// send frame
				bridge.getCanConnection().send(this.canFrame);
				this.canFrame.onCanFrameSuccessfullySend();

				// this is a workaround for container auto requester mechanism
				while (this.theElement != null && this.theElement.sendChunkOfFrames()) {
					try {
						Thread.sleep(7);
					} catch (InterruptedException e) {
						;
					}
					bridge.getCanConnection().send(this.canFrame);
					this.canFrame.onCanFrameSuccessfullySend();
				}
			}
		} catch (OpenemsException e) {
			// TODO evaluate: log error vs throw exception
			this.logEvt.error("Got Exception CAN send failed " + e.getMessage());
		}

	}

	@Override
	public int _execute(AbstractCanBridge bridge) throws OpenemsException {
		this.debugLog("_ececute");

		// Measure the actual cycle-time; and starts the next measure cycle
		if (this.cycleStopwatch.isRunning()) {
			this.taskCycleTime = this.cycleStopwatch.elapsed(TimeUnit.MILLISECONDS);
		}
		if (!this.canFrame.isAllowedTx()) {
			if (this.cycleStopwatch.isRunning()) {
				this.cycleStopwatch.stop();
			}
			return 1;
		}
		this.cycleStopwatch.reset();
		this.cycleStopwatch.start();
		try {
			this.sendCanFrame(bridge);
		} catch (CanException e) {
			throw new OpenemsException(e);
		}
		return 1;
	}

	@Override
	public Priority getPriority() {
		return this.priority;
	}

	@Override
	protected String getDescription() {
		return "CanFrameWriteTask";
	}

	private void debugLog(String txt) {
		if (this.getLogVerbosity() == LogVerbosity.ALL) {
			this.logCan.info(this.getDescription() + " " + txt + " CAN adr: " + this.getCanAddress() + " expcycle "
					+ this.getCanExpectedCycleTime() + "ms");
		}
	}

}
