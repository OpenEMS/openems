package io.openems.edge.api.device;

public enum DeviceMessageType {
	/**
	 * General runtime information of the Device
	 */
	INFO,
	/**
	 * Warning with information for the end-user 
	 */
	USERWARNING,
	/**
	 * Warning with information for a technician 
	 */
	HARDWAREWARNING,
	/**
	 * Error with information for the end-user
	 */
	USERERROR,
	/**
	 * Error with information for a technician
	 */
	HARDWAREERROR,
	/**
	 * Debug information for the developer
	 */
	DEBUG
}
