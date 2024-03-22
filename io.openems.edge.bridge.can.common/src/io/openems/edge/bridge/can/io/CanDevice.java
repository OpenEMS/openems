package io.openems.edge.bridge.can.io;

import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.bridge.can.api.data.CanSimulationData;
import io.openems.edge.bridge.can.io.hw.CanDeviceException;

public interface CanDevice {

	/**
	 * Asks if the CAN device is ready.
	 *
	 * @return true, if the CAN device is ready
	 */
	public boolean isReady();

	/**
	 * Opens the CAN connection.
	 *
	 * @param baudrate the baudrate
	 * @throws CanDeviceException on error
	 */
	public void open(int baudrate) throws CanDeviceException;

	/**
	 * Closes the CAN connection.
	 *
	 * @throws CanDeviceException on error
	 */
	public void close() throws CanDeviceException;

	/**
	 * Sets the baudrate.
	 *
	 * @param baudRate the new baudrate
	 * @throws CanDeviceException on error
	 */
	public void setBaudrate(int baudRate) throws CanDeviceException;

	/**
	 * Receives all data.
	 *
	 * @return an array of {@link CanRxTxData}
	 * @throws CanDeviceException on error
	 */
	public CanRxTxData[] receiveAll() throws CanDeviceException;

	/**
	 * Sends a CAN frame.
	 *
	 * @param transmitFrame the frame to be sent
	 * @throws CanDeviceException on error
	 */
	public void send(CanRxTxData transmitFrame) throws CanDeviceException;

	/**
	 * Enables the simulation mode.
	 *
	 * @param simuData the simulated data
	 */
	public void enableSimulationMode(CanSimulationData simuData);

	/**
	 * Adds a frame to the list of frames sent cyclically.
	 *
	 * @param transmitFrame the frame
	 * @param cycleTime     the time (in ms) after which the frame should be resent
	 * @throws CanDeviceException on error
	 */
	public void sendCyclicallyAdd(CanRxTxData transmitFrame, int cycleTime) throws CanDeviceException;

	/**
	 * Removes a frame from the list of frames sent cyclically.
	 *
	 * @param transmitFrame the frame
	 * @throws CanDeviceException on error
	 */
	public void sendCyclicallyRemove(CanRxTxData transmitFrame) throws CanDeviceException;

	/**
	 * Updates a cyclically sent frame and sends it.
	 *
	 * @param transmitFrame the frame to be updated
	 * @throws CanDeviceException on error
	 */
	public void sendCyclicallyAdopt(CanRxTxData transmitFrame) throws CanDeviceException;

	/**
	 * Removes all cyclically sent frames.
	 *
	 * @throws CanDeviceException on error
	 */
	public void removeCyclicalAll() throws CanDeviceException;

	/**
	 * Enables cyclical auto incrementing of a byte.
	 *
	 * @param canAddress             the address
	 * @param autoIncrementByteIndex the index of the byte to be incremented
	 * @throws CanDeviceException on error
	 */
	public void enableCyclicallyAutoIncrement(int canAddress, int autoIncrementByteIndex) throws CanDeviceException;

	/**
	 * Gets the CAN frame error counter for cyclically sent CAN frames.
	 *
	 * @return the number of errors in cyclically sent CAN frames
	 * @throws CanDeviceException on error
	 */
	public int statsGetCanFrameErrorCntrCyclicalSend() throws CanDeviceException;

	/**
	 * Gets the CAN frame error counter for sent CAN frames.
	 *
	 * @return the number of errors in sent CAN frames
	 * @throws CanDeviceException on error
	 */
	public int statsGetCanFrameErrorCntrSend() throws CanDeviceException;

	/**
	 * Gets the CAN frame error counter for received CAN frames.
	 *
	 * @return the number of errors in cyclically received CAN frames
	 * @throws CanDeviceException on error
	 */
	public int statsGetCanFrameErrorCntrReceive() throws CanDeviceException;

	/**
	 * Gets the number of CAN frames sent per cycle.
	 *
	 * @return the number of CAN frames sent per cycle
	 * @throws CanDeviceException on error
	 */
	public int statsGetCanFrameFramesSendPerCycle() throws CanDeviceException;

}
