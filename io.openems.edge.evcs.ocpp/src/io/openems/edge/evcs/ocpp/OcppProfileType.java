package io.openems.edge.evcs.ocpp;

public enum OcppProfileType {
	
	/**
	 * Basic Charge Point functionality comparable with OCPP 1.5 without support for firmware updates, local authorization list management and reservations.
	 */
	CORE, 
	
	/**
	 * Firmware Management Support for firmware update management and diagnostic log file download.
	 */
	FIRMWARE_MANAGEMENT, 
	
	/**
	 * Features to manage the local authorization list in Charge Points.
	 */
	LOCAL_AUTH_LIST_MANAGEMENT, 
	
	/**
	 * Support for reservation of a Charge Point.
	 */
	RESERVATION, 	
	
	/**
	 * Support for basic Smart Charging, for instance using control pilot.
	 */
	SMART_CHARGING, 
	
	/**
	 * Support for remote triggering of Charge Point initiated messages
	 */
	REMOTE_TRIGGER; 

	private OcppProfileType() {
	}
	
}
