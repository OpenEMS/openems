package io.openems.edge.bridge.can.api.data;

import io.openems.edge.bridge.can.api.element.CanChannelElement;

/**
 * A CanFrame represents a low level CAN frame to send/receive<br/>
 * <br/>
 *
 * <p>
 * To each CAN frame a set of {@link CanChannelElement} are linked. The
 * {@link CanChannelElement} are responsible for a part of the data within this
 * very specific CAN frame.
 */
public interface CanFrame extends CanRxTxData {

	/**
	 * Gets all the {@link CanChannelElement}s of the frame.
	 *
	 * @return an array containing the associated CanChannelElements
	 */
	public CanChannelElement<?>[] getElements();

	public void setExtendedAddress(boolean b);

}
