package io.openems.common;

/**
 * Adjustments for OpenEMS OEM distributions.
 */
// CHECKSTYLE:OFF
public class OpenemsOEM {
	// CHECKSTYLE:ON
	public enum Manufacturer {
		OPENEMS; //
	}

	/*
	 * General.
	 */
	public static final String MANUFACTURER = "OpenEMS Association e.V.";

	/*
	 * Backend-Api Controller
	 */
	public static final String BACKEND_API_URI = "ws://localhost:8081";

	/*
	 * System-Update.
	 */
	/**
	 * Name of the Debian package.
	 */
	public static final String SYSTEM_UPDATE_PACKAGE = "none";
	public static final String SYSTEM_UPDATE_LATEST_VERSION_URL = "none";
	public static final String SYSTEM_UPDATE_SCRIPT_URL = "none";

	/*
	 * Backend InfluxDB.
	 */
	public static final String INFLUXDB_TAG = "edge";

}
