package io.openems.edge.bridge.can.api.data;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Stopwatch;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.can.api.CanIoException;
import io.openems.edge.bridge.can.api.CanUtils;
import io.openems.edge.bridge.can.api.element.AbstractCanChannelElement;
import io.openems.edge.bridge.can.api.element.CanChannelElement;
import io.openems.edge.bridge.can.api.task.AbstractTask;

/**
 * A {@link CanFrame} representing exactly one CAN frame.
 */
public class CanFrameImpl implements CanFrame {

	private final Logger logEvt = LoggerFactory.getLogger("Prototype.Can.Event");

	private static final boolean debugMaxCycleTime = true;

	/** the task responsible for this CanFrame. */
	private boolean isExtendedAddress;
	private int address;
	private int expectedCycleTime; // ms
	private byte[] data;

	private boolean txAllowed = true;

	public static final int MIN_CYCLE_TIME_BEFORE_LOGGING = 200_000;

	// Measures the max time elapsed between two received CAN msgs
	private final Stopwatch timeBetweenRecvdCanMsgs = Stopwatch.createUnstarted();
	private static long maxCycleTime = MIN_CYCLE_TIME_BEFORE_LOGGING;

	private AbstractCanChannelElement<?, ?>[] elements;

	public CanFrameImpl(CanRxTxData in) {
		this.address = in.getAddress();
		this.isExtendedAddress = in.isExtendedAddress();
		this.data = in.getData();
	}

	public CanFrameImpl(AbstractTask referencedTask, boolean isExtendedAddress, int canFrameAddress,
			int expectedCycleTime, AbstractCanChannelElement<?, ?>[] elements) {

		this.isExtendedAddress = isExtendedAddress;
		this.address = canFrameAddress;
		this.expectedCycleTime = expectedCycleTime;
		this.elements = elements;
		this.initCanData();
		for (AbstractCanChannelElement<?, ?> element : elements) {
			element.setCanFrame(this);
		}
	}

	private void initCanData() {
		this.data = new byte[8];
		for (var i = 0; i < this.data.length; i++) {
			this.data[i] = 0;
		}
	}

	@Override
	public boolean isExtendedAddress() {
		return this.isExtendedAddress;
	}

	@Override
	public void setExtendedAddress(boolean isExtAdr) {
		this.isExtendedAddress = isExtAdr;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	@Override
	public int getAddress() {
		return this.address;
	}

	@Override
	public void setAddress(int addr) {
		this.address = addr;
	}

	@Override
	public byte[] getData() {
		return this.data;
	}

	@Override
	public int getLength() {
		if (this.data != null) {
			return this.data.length;
		}
		return 0;
	}

	public int getExpectedCycleTime() {
		return this.expectedCycleTime;
	}

	@Override
	public CanChannelElement<?>[] getElements() {
		return this.elements;
	}

	private void measureTimeBetweenTwoReceivedMsgs() {
		if (!debugMaxCycleTime) {
			return;
		}
		if (!this.timeBetweenRecvdCanMsgs.isRunning()) {
			this.timeBetweenRecvdCanMsgs.reset();
			this.timeBetweenRecvdCanMsgs.start();
			return;
		}
		var cycleTime = this.timeBetweenRecvdCanMsgs.elapsed(TimeUnit.MILLISECONDS);
		if (cycleTime > maxCycleTime) {
			maxCycleTime = cycleTime;
			this.logEvt.warn(String.format("Max cycle time for CAN frame with address 0x%08x:  %d ms", this.address,
					maxCycleTime));
		}
		this.timeBetweenRecvdCanMsgs.reset();
		this.timeBetweenRecvdCanMsgs.start();
	}

	/**
	 * This method is called when a new CAN frame is received via low level
	 * hardware. It measures the time between two received messages and collects the
	 * data.
	 *
	 * @param rawFrame the {@link CanRxTxData}
	 */
	public void onUpdateData(CanRxTxData rawFrame) {
		if (rawFrame == null || this.getData() != null && this.getLength() != rawFrame.getLength()) {
			this.logEvt.warn("No frame or frame with invalid length received");
			return;
		}
		this.measureTimeBetweenTwoReceivedMsgs();

		this.data = rawFrame.getData();

		// handle channel elements
		for (AbstractCanChannelElement<?, ?> canElement : this.elements) {
			try {
				canElement.doElementSetInput(this.data);
			} catch (CanIoException e) {
				this.logEvt.error("Error updating channels from " + this.toString() + " ex: " + e.getMessage());
			}
		}
	}

	/**
	 * Is called when a CAN frame could be send successfully via low level hardware.
	 *
	 * @throws OpenemsException on error
	 */
	public void onCanFrameSuccessfullySend() throws OpenemsException {
		for (CanChannelElement<?> element : this.getElements()) {
			element.onCanFrameSuccessfullySend();
		}
	}

	@Override
	public String toString() {
		return "CanFrame [" + String.format("%7d/X%06x %d", this.address, this.address, this.getLength()) + ", "
				+ CanUtils.getHexInfo(this.getData()) + ", " + (this.isExtendedAddress ? "Extd" : "Std ") + "]";
	}

	/*
	 * Enable Debug mode for this Element. Activates verbose logging.
	 */
	private boolean isDebug = false;

	/**
	 * Turns on the debug mode.
	 *
	 * @return itself
	 */
	public CanFrameImpl debug() {
		this.isDebug = true;
		return this;
	}

	protected boolean isDebug() {
		return this.isDebug;
	}

	public void setAllowedTx(boolean isAllwed) {
		if (this.txAllowed != isAllwed) {
			this.logEvt.info("CAN Frame: txAllowed switched to " + isAllwed);
		}
		this.txAllowed = isAllwed;
	}

	public boolean isAllowedTx() {
		return this.txAllowed;
	}

}
