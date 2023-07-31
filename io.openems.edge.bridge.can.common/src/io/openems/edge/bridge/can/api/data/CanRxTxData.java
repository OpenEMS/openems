package io.openems.edge.bridge.can.api.data;

/**
 * Represents a low level driver specific CAN frame to send/receive.
 */
public interface CanRxTxData {

	/**
	 * Gets the address of this Can element.
	 *
	 * @return the CAN address
	 */
	public int getAddress();

	/**
	 * Sets the address of this Can element.
	 *
	 * @param addr the address
	 */
	public void setAddress(int addr);

	/**
	 * Asks if the address is an extended CAN frame.
	 *
	 * @return true, if the address is an extended CAN frame (29-bit), false if it
	 *         is a standard frame (11-bit)
	 */
	public boolean isExtendedAddress();

	/**
	 * Gets the length of the CAN frame data array.
	 *
	 * @return the length
	 */
	public int getLength();

	/**
	 * Gets the data as bytes.
	 *
	 * @return 0 -8 data bytes
	 */
	public byte[] getData();

}
