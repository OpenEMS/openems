package io.openems.edge.bridge.mbus.api;

import org.openmuc.jmbus.MBusConnection;

public interface BridgeMbus {

	/**
	 * Register a device with the M-Bus bridge. The bridge will then poll this device.
	 *
	 * @param sourceId the Id of the module.
	 * @param task the MbusTask, containing all the information about the device.
	 */
	public void addTask(String sourceId, MbusTask task);

	/**
	 * Get the M-Bus connection of the bridge.
	 *
	 * @return the MBusConnection.
	 */
	public MBusConnection getmBusConnection();

	/**
	 * Remove a device from the M-Bus bridge. The bridge will stop polling this device.
	 *
	 * @param sourceId the Id of the module.
	 */
	public void removeTask(String sourceId);
}
