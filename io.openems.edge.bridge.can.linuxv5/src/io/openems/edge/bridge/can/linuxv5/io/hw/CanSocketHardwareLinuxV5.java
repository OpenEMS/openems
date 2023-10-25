package io.openems.edge.bridge.can.linuxv5.io.hw;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.data.CanSimulationData;
import io.openems.edge.bridge.can.io.CanDevice;
import io.openems.edge.bridge.can.io.hw.CanDeviceException;
import io.openems.edge.socketcan.driver.CanSocket;
import io.openems.edge.socketcan.driver.CanSocket.CanFrame;
import io.openems.edge.socketcan.driver.CanSocket.CanInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Delivers access to the real CAN hardware.
 * 
 * <p>
 * Access currently is provided by a CAN Socket implementation running on a
 * Kunbus RevPi CAN extension board
 */
public class CanSocketHardwareLinuxV5 implements CanDevice {
	private final Logger log = LoggerFactory.getLogger(CanSocketHardwareLinuxV5.class);

	// linux CAN Socket interface
	private static final String CAN_INTERFACE = "can0";
	private static final int CAN_READ_TIMEOUT = 100;// us

	private boolean deviceOpened = false;

	private CanSocket theCanSocket;
	private CanInterface theCanInterface;

	public CanSocketHardwareLinuxV5() {
		this.deviceOpened = false;
	}

	@Override
	public boolean isReady() {
		return (this.deviceOpened && this.theCanSocket != null);
	}

	@Override
	public void open(int baudrate) throws CanDeviceException {
		try {
			final var socket = new CanSocket(CanSocket.Mode.RAW);
			final var canif = new CanInterface(socket, CAN_INTERFACE);
			this.theCanSocket = socket;
			this.theCanInterface = canif;
			this.theCanSocket.bind(this.theCanInterface);
			this.theCanSocket.setReceiveTimeout(0, CAN_READ_TIMEOUT);

			this.log.info("CAN Socket Hardware library loaded and initialized");
		} catch (Exception e) {
			this.log.error("Got Exception on CAN frame: " + e.getMessage());
			e.printStackTrace();
		}

		this.setBaudrate(baudrate);
		this.deviceOpened = true;
	}

	@Override
	public void close() throws CanDeviceException {
		try {
			if (this.deviceOpened && this.theCanSocket != null) {
				this.theCanSocket.close();
			}
			this.theCanSocket = null;
			this.deviceOpened = false;
		} catch (IOException e) {
			this.theCanSocket = null;
			this.deviceOpened = false;
			e.printStackTrace();
			throw new CanDeviceException("Unable to close the CAN socket Ex: " + e.getMessage());
		}
	}

	@Override
	public void setBaudrate(int baudrate) throws CanDeviceException {
		; // unused, Note baudRate on Kunbus RevPi needs to be set by CAN initialisation
	}

	private CanRxTxData can2can(final CanFrame frame) {
		if (frame == null) {
			return null;
		}
		return new CanRxTxData() {
			@Override
			public boolean isExtendedAddress() {
				if (this.getAddress() > 2047) {
					return true;
				}
				return false;
			}

			@Override
			public int getLength() {
				return (frame.getData() != null) ? frame.getData().length : 0;
			}

			@Override
			public byte[] getData() {
				return frame.getData();
			}

			@Override
			public int getAddress() {
				return frame.getCanId().getCanId();
			}

			@Override
			public void setAddress(int addr) {
			}
		};
	}

	// will be called once every cycle
	@Override
	public synchronized CanRxTxData[] receiveAll() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		CanRxTxData[] rxDat = null;

		try {
			List<CanFrame> in = new ArrayList<>();
			while (this.isReady()) {
				try {
					var frame = this.theCanSocket.recv();
					if (frame != null) {
						in.add(frame);
					} else {
						break;
					}
				} catch (Exception e) {
					// this is thrown on recv timeout
					break;
				}
			}
			if (in.size() > 0) {
				rxDat = new CanRxTxData[in.size()];
				for (var i = 0; i < in.size(); i++) {
					rxDat[i] = this.can2can(in.get(i));
				}
				in = null;
			}
		} catch (Exception e) {
			this.log.error("Got Exception while receiving CAN frame: " + e.getMessage());
			throw new CanDeviceException("Error receiving CAN frame Ex: " + e.getMessage());
		}
		return rxDat;
	}

	@Override
	public synchronized void send(CanRxTxData transmitFrame) throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {

			var id = new CanSocket.CanId(transmitFrame.getAddress());
			if (transmitFrame.isExtendedAddress()) {
				id = new CanSocket.CanId(0x80000000 | transmitFrame.getAddress());
			}

			this.theCanSocket.send(new CanFrame(this.theCanInterface, id, transmitFrame.getData()));

		} catch (Exception e) {
			this.log.error("Got Exception on sending CAN frame: " + e.getMessage());
			throw new CanDeviceException("Error sending CAN frame " + transmitFrame + " Ex: " + e.getMessage());
		}
	}

	@Override
	public void enableSimulationMode(CanSimulationData simuData) {
		; // unused
	}

	@Override
	public void sendCyclicallyAdd(CanRxTxData transmitFrame, int cycleTime) throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			var id = new CanSocket.CanId(transmitFrame.getAddress());
			if (transmitFrame.isExtendedAddress()) {
				id = new CanSocket.CanId(0x80000000 | transmitFrame.getAddress());
			}
			this.theCanSocket.sendCyclicallyAdd(new CanFrame(this.theCanInterface, id, transmitFrame.getData()),
					cycleTime);

		} catch (Exception e) {
			this.log.error("Got Exception on doing sendCyclicallyAdd: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing sendCyclicallyAdd " + transmitFrame + " Ex: " + e.getMessage());
		}
	}

	@Override
	public void sendCyclicallyRemove(CanRxTxData transmitFrame) throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			var id = new CanSocket.CanId(transmitFrame.getAddress());
			if (transmitFrame.isExtendedAddress()) {
				id = new CanSocket.CanId(0x80000000 | transmitFrame.getAddress());
			}
			this.theCanSocket.sendCyclicallyRemove(new CanFrame(this.theCanInterface, id, transmitFrame.getData()));

		} catch (Exception e) {
			this.log.error("Got Exception on doing sendCyclicallyRemove: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing sendCyclicallyRemove " + transmitFrame + " Ex: " + e.getMessage());
		}
	}

	@Override
	public void sendCyclicallyAdopt(CanRxTxData transmitFrame) throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			var id = new CanSocket.CanId(transmitFrame.getAddress());
			if (transmitFrame.isExtendedAddress()) {
				id = new CanSocket.CanId(0x80000000 | transmitFrame.getAddress());
			}
			this.theCanSocket.sendCyclicallyAdopt(new CanFrame(this.theCanInterface, id, transmitFrame.getData()));

		} catch (Exception e) {
			this.log.error("Got Exception on doing sendCyclicallyAdopt: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing sendCyclicallyAdopt " + transmitFrame + " Ex: " + e.getMessage());
		}
	}

	@Override
	public void removeCyclicalAll() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			this.theCanSocket.removeCyclicalAll();

		} catch (Exception e) {
			this.log.error("Got Exception on doing removeCyclicalAll: " + e.getMessage());
			throw new CanDeviceException("Got Exception on doing removeCyclicalAll Ex: " + e.getMessage());
		}
	}

	@Override
	public void enableCyclicallyAutoIncrement(int canAddress, int autoIncrementByteIndex) throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			this.theCanSocket.enableCyclicallyAutoIncrement(canAddress, autoIncrementByteIndex);

		} catch (Exception e) {
			this.log.error("Got Exception on doing enableCyclicallyAutoIncrement: " + e.getMessage());
			throw new CanDeviceException("Got Exception on doing enableCyclicallyAutoIncrement Ex: " + e.getMessage());
		}
	}

	@Override
	public int statsGetCanFrameErrorCntrCyclicalSend() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			return this.theCanSocket.statsGetCanFrameErrorCntrCyclicalSend();

		} catch (Exception e) {
			this.log.error("Got Exception on doing statsGetCanFrameErrorCntrCyclicalSend: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing statsGetCanFrameErrorCntrCyclicalSend Ex: " + e.getMessage());
		}
	}

	@Override
	public int statsGetCanFrameErrorCntrSend() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			return this.theCanSocket.statsGetCanFrameErrorCntrSend();

		} catch (Exception e) {
			this.log.error("Got Exception on doing statsGetCanFrameErrorCntrSend: " + e.getMessage());
			throw new CanDeviceException("Got Exception on doing statsGetCanFrameErrorCntrSend Ex: " + e.getMessage());
		}
	}

	@Override
	public int statsGetCanFrameErrorCntrReceive() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			return this.theCanSocket.statsGetCanFrameErrorCntrReceive();

		} catch (Exception e) {
			this.log.error("Got Exception on doing statsGetCanFrameErrorCntrReceive: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing statsGetCanFrameErrorCntrReceive Ex: " + e.getMessage());
		}
	}

	@Override
	public int statsGetCanFrameFramesSendPerCycle() throws CanDeviceException {
		if (!this.deviceOpened || this.theCanSocket == null) {
			throw new CanDeviceException("No CAN socket");
		}
		try {
			return this.theCanSocket.statsGetCanFrameFramesSendPerCycle();

		} catch (Exception e) {
			this.log.error("Got Exception on doing statsGetCanFrameFramesSendPerCycle: " + e.getMessage());
			throw new CanDeviceException(
					"Got Exception on doing statsGetCanFrameFramesSendPerCycle Ex: " + e.getMessage());
		}
	}

}
