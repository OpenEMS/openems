package io.openems.edge.bridge.can.io;

import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.data.CanSimulationData;
import io.openems.edge.bridge.can.io.hw.CanDeviceException;

/**
 * CAN driver for cyclically simulating CAN Frame send/response activity.
 */
public class CanSimulator implements CanDevice {

	private CanSimulationData canSimulation = null;

	private boolean deviceOpened = false;

	public CanSimulator() {
		this.deviceOpened = false;
	}

	@Override
	public void enableSimulationMode(CanSimulationData simuData) {
		this.canSimulation = simuData;
	}

	@Override
	public boolean isReady() {
		return this.deviceOpened;
	}

	@Override
	public void open(int baudrate) throws CanDeviceException {
		this.setBaudrate(baudrate);
		this.deviceOpened = true;
	}

	@Override
	public void close() throws CanDeviceException {
		this.deviceOpened = false;
	}

	@Override
	public void setBaudrate(int baudRate) throws CanDeviceException {
	}

	// will be called once every scheduler cycle
	@Override
	public synchronized CanRxTxData[] receiveAll() throws CanDeviceException {
		CanRxTxData[] rxDat = null;
		if (this.canSimulation != null) {
			return this.canSimulation.receiveData();
		}
		return rxDat;
	}

	@Override
	public synchronized void send(CanRxTxData transmitFrame) throws CanDeviceException {
		if (this.canSimulation != null) {
			this.canSimulation.sendData(transmitFrame);
		}
	}

	@Override
	public void sendCyclicallyAdd(CanRxTxData transmitFrame, int cycleTime) throws CanDeviceException {
	}

	@Override
	public void sendCyclicallyRemove(CanRxTxData transmitFrame) throws CanDeviceException {
	}

	@Override
	public void sendCyclicallyAdopt(CanRxTxData transmitFrame) throws CanDeviceException {
	}

	@Override
	public void removeCyclicalAll() throws CanDeviceException {
	}

	@Override
	public void enableCyclicallyAutoIncrement(int canAddress, int autoIncrementByteIndex) throws CanDeviceException {
	}

	@Override
	public int statsGetCanFrameErrorCntrCyclicalSend() throws CanDeviceException {
		// not used
		return 0;
	}

	@Override
	public int statsGetCanFrameErrorCntrSend() throws CanDeviceException {
		// not used
		return 0;
	}

	@Override
	public int statsGetCanFrameErrorCntrReceive() throws CanDeviceException {
		// not used
		return 0;
	}

	@Override
	public int statsGetCanFrameFramesSendPerCycle() throws CanDeviceException {
		// not used
		return 0;
	}
}
