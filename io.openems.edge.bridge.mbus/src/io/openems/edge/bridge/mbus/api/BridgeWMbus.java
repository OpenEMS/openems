package io.openems.edge.bridge.mbus.api;

public interface BridgeWMbus {

	/**
	 * Register a device with the Wireless M-Bus bridge. The bridge will then listen to messages from this device.
	 *
	 * @param sourceId the Id of the module.
	 * @param protocol the WMbusProtocol, containing all the information about the device.
	 */
	public void addProtocol(String sourceId, WMbusProtocol protocol);

	/**
	 * Remove a device from the Wireless M-Bus bridge. The bridge will stop listening to messages from this device.
	 *
	 * @param sourceId the Id of the module.
	 */
	public void removeProtocol(String sourceId);
}
