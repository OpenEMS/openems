package io.openems.edge.bridge.can.api.data;

public interface CanSimulationData {
	/**
	 * Receives the simulated data.
	 *
	 * @return An array of the received CAN frames
	 */
	public CanRxTxData[] receiveData();

	/**
	 * Simulates the transmission of a frame.
	 *
	 * @param transmitFrame the frame to be transmitted
	 */
	public void sendData(CanRxTxData transmitFrame);
}
