package io.openems.edge.api.message;

public enum MessageType {
	/**
	 * General runtime information of the Device
	 */
	INFO,
	/**
	 * Warning with information for the end-user 
	 */
	USERWARNING,
	/**
	 * Error with information for the end-user
	 */
	USERERROR,
	/**
	 * Warning with information for a technician 
	 */
	TECHNICIANWARNING,
	/**
	 * Error with information for a technician
	 */
	TECHNICIANERROR,
	/**
	 * Debug information for the developer
	 */
	DEBUG
}
