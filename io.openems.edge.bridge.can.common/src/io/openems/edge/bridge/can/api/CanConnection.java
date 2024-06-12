package io.openems.edge.bridge.can.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.bridge.can.AbstractCanBridge;
import io.openems.edge.bridge.can.CanHardwareType;
import io.openems.edge.bridge.can.api.data.CanFrame;
import io.openems.edge.bridge.can.api.data.CanFrameImpl;
import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.data.CanSimulationData;
import io.openems.edge.bridge.can.io.CanDevice;
import io.openems.edge.bridge.can.io.hw.CanDeviceException;

/**
 * Wrapper around the hardware CAN driver.
 */
public class CanConnection {

	private final Logger log = LoggerFactory.getLogger(CanConnection.class);

	private CanDevice canDevice;
	private final AbstractCanBridge bridge;
	private final CanHardwareType selectedHardware;
	private final CanDevice theHardwareRef;

	public CanConnection(AbstractCanBridge bridge, CanHardwareType selectedHardware, CanDevice theHardwareRef) {
		this.canDevice = null;
		this.bridge = bridge;
		this.selectedHardware = selectedHardware;
		this.theHardwareRef = theHardwareRef;
	}

	/**
	 * Opens the CAN connection.
	 *
	 * @param baudrate the baudrate
	 * @throws Exception if there is a driver failure.
	 */
	public void connect(int baudrate) throws CanIoException {
		if (this.canDevice != null) {
			this.close();
		}
		try {
			this.canDevice = this.theHardwareRef;
			this.canDevice.open(baudrate);
			this.log.warn("CanConnection connected");
		} catch (CanDeviceException e) {
			this.log.error("ERROR: CanConnection could not be opened: " + e.getMessage());
			throw new CanIoException("ERROR: CanConnection could not be opened: " + e.getMessage());
		}
	}

	/**
	 * Closes the connection.
	 *
	 * @throws CanIoException on error
	 */
	public void close() throws CanIoException {
		if (this.canDevice != null) {
			this.log.warn("CanConnection closed");
			try {
				this.canDevice.close();
				this.canDevice = null;
			} catch (CanDeviceException e) {
				this.canDevice = null;
				this.log.error("ERROR: CanConnection could not be closed : " + e.getMessage());
				throw new CanIoException("ERROR: CanConnection could not be closed: " + e.getMessage());
			}
		}
	}

	/**
	 * Asks if the connection is open.
	 *
	 * @return true, if the connection is open.
	 */
	public boolean isOpen() {
		if (this.canDevice != null) {
			return this.canDevice.isReady();
		}
		return false;
	}

	/**
	 * Receives all data.
	 *
	 * @return The data as {@link CanFrame}-array
	 * @throws CanIoException if the connection is not open or
	 */
	public CanFrame[] receiveAll() throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			var rxTx = this.canDevice.receiveAll();
			if (rxTx != null) {
				CanFrame[] frames = new CanFrameImpl[rxTx.length];
				for (var i = 0; i < rxTx.length; i++) {
					frames[i] = new CanFrameImpl(rxTx[i]);
					this.debugLog(true, frames[i]);
				}
				return frames;
			}
			return null;
		} catch (CanDeviceException e) {
			throw new CanIoException("No data available");
		}
	}

	/**
	 * Sends a Can frame.
	 *
	 * @param canFrame the {@link CanFrame}
	 * @throws CanIoException on error
	 */
	public void send(CanFrame canFrame) throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			this.debugLog(false, canFrame);
			this.canDevice.send(canFrame);
		} catch (CanDeviceException e) {
			throw new CanIoException("Unable to send can frame " + canFrame.toString() + " ex: " + e.getMessage());
		}
	}

	private void debugLog(boolean isRead, CanFrame frame) {
		switch (this.bridge.getLogVerbosity()) {
		case ALL:
		case READS_AND_WRITES:
			if (isRead) {
				this.bridge.logInfo(this.log, "<-RX CAN Frame:" + frame.toString());
			} else {
				this.bridge.logInfo(this.log, "TX-> CAN Frame:" + frame.toString());
			}
			break;
		case READS:
			if (isRead) {
				this.bridge.logInfo(this.log, "<-RX CAN Frame:" + frame.toString());
			}
			break;
		case WRITES:
			if (!isRead) {
				this.bridge.logInfo(this.log, "TX-> CAN Frame:" + frame.toString());
			}
			break;
		case NONE:
			break;
		}
	}

	/**
	 * Enables the CAN sumilation.
	 *
	 * @param simu the {@link CanSimulationData}
	 */
	public void enableSimulation(CanSimulationData simu) {
		if (this.selectedHardware == CanHardwareType.SIMULATOR) {
			this.bridge.logInfo(this.log, "---CAN Simulation Activated---");
			this.canDevice.enableSimulationMode(simu);
		}
	}

	/**
	 * Adds a frame to the list of frames sent cyclically.
	 *
	 * @param canFrame  the frame
	 * @param cycleTime the time (in ms) after which the frame should be resent
	 * @throws CanIoException on error
	 */
	public void sendCyclicallyAdd(CanFrameImpl canFrame, int cycleTime) throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			this.canDevice.sendCyclicallyAdd(canFrame, cycleTime);
		} catch (CanDeviceException e) {
			throw new CanIoException(
					"Unable to do send cyclically add for frame " + canFrame.toString() + " ex: " + e.getMessage());
		}
	}

	/**
	 * Updates a cyclically sent frame and sends it.
	 *
	 * @param canFrame the frame to be updated
	 * @throws CanDeviceException on error
	 */
	public void sendCyclicallyAdopt(CanFrameImpl canFrame) throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("Device is closed.");
		}
		try {
			this.canDevice.sendCyclicallyAdopt(canFrame);
		} catch (CanDeviceException e) {
			throw new CanIoException(
					"Unable to do cyclically adopt for frame " + canFrame.toString() + " ex: " + e.getMessage());
		}
	}

	/**
	 * Removes a frame from the list of frames sent cyclically.
	 *
	 * @param canFrame the frame
	 * @throws CanIoException on error
	 */
	public void sendCyclicallyRemove(CanRxTxData canFrame) throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			this.canDevice.sendCyclicallyRemove(canFrame);
		} catch (CanDeviceException e) {
			throw new CanIoException(
					"Unable to do send cyclically remove for frame " + canFrame.toString() + " ex: " + e.getMessage());
		}
	}

	/**
	 * Removes all cyclically sent frames.
	 *
	 * @throws CanIoException on error
	 */
	public void removeCyclicalAll() throws CanIoException {
		if (this.isOpen()) {
			try {
				this.canDevice.removeCyclicalAll();
			} catch (CanDeviceException e) {
				throw new CanIoException("Unable to do remove cyclically Send  ex: " + e.getMessage());
			}
		}
	}

	/**
	 * Enables cyclical auto incrementing of a byte.
	 *
	 * @param canAddress             the address
	 * @param autoIncrementByteIndex the index of the byte to be incremented
	 * @throws CanIoException on error
	 */
	public void enableCyclicallyAutoIncrement(int canAddress, int autoIncrementByteIndex) throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			this.canDevice.enableCyclicallyAutoIncrement(canAddress, autoIncrementByteIndex);
		} catch (CanDeviceException e) {
			throw new CanIoException(
					"Unable to enable cyclical autoincrement for canAddress " + canAddress + " ex: " + e.getMessage());
		}
	}

	/**
	 * Gets the CAN frame error counter for cyclically sent CAN frames.
	 *
	 * @return the number of errors in cyclically sent CAN frames
	 * @throws CanIoException on error
	 */
	public int statsGetCanFrameErrorCntrCyclicalSend() throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			return this.canDevice.statsGetCanFrameErrorCntrCyclicalSend();
		} catch (CanDeviceException e) {
			throw new CanIoException("Unable to get statsGetCanFrameErrorCntrCyclicalSend ex: " + e.getMessage());
		}
	}

	/**
	 * Gets the CAN frame error counter for sent CAN frames.
	 *
	 * @return the number of errors in sent CAN frames
	 * @throws CanIoException on error
	 */
	public int statsGetCanFrameErrorCntrSend() throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			return this.canDevice.statsGetCanFrameErrorCntrSend();
		} catch (CanDeviceException e) {
			throw new CanIoException("Unable to get statsGetCanFrameErrorCntrSend ex: " + e.getMessage());
		}
	}

	/**
	 * Gets the CAN frame error counter for received CAN frames.
	 *
	 * @return the number of errors in cyclically received CAN frames
	 * @throws CanIoException on error
	 */
	public int statsGetCanFrameErrorCntrReceive() throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			return this.canDevice.statsGetCanFrameErrorCntrReceive();
		} catch (CanDeviceException e) {
			throw new CanIoException("Unable to get statsGetCanFrameErrorCntrReceive ex: " + e.getMessage());
		}
	}

	/**
	 * Gets the number of CAN frames sent per cycle.
	 *
	 * @return the number of CAN frames sent per cycle
	 * @throws CanIoException on error
	 */
	public int statsGetCanFrameFramesSendPerCycle() throws CanIoException {
		if (!this.isOpen()) {
			throw new CanIoException("device is closed");
		}
		try {
			return this.canDevice.statsGetCanFrameFramesSendPerCycle();
		} catch (CanDeviceException e) {
			throw new CanIoException("Unable to get statsGetCanFrameFramesSendPerCycle ex: " + e.getMessage());
		}
	}

}
