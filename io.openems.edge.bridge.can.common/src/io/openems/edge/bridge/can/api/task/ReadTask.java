package io.openems.edge.bridge.can.api.task;

import io.openems.edge.bridge.can.api.data.CanRxTxData;
import io.openems.edge.common.taskmanager.ManagedTask;

/**
 * A 'ReadTask' is holding references to one {@link AbstractCanFrame} object.
 * The ReadTask handles the evaluation of the received CAN Frame and also
 * updates the ChannelElements appropriately
 */
public interface ReadTask extends Task, ManagedTask {

	/**
	 * The {@link CanWorker} uses this method to deliver incoming CAN frames with
	 * the CAN address specified within this task.
	 *
	 * @param rxData the received CAN data frame
	 */
	public void onReceivedFrame(CanRxTxData rxData);

}
