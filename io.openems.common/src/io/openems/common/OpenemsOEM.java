package io.openems.common;

/**
 * Adjustments for OpenEMS OEM distributions.
 */
// CHECKSTYLE:OFF
public class OpenemsOEM {
	// CHECKSTYLE:ON

	/*
	 * General.
	 */
	public final static String MANUFACTURER = "FENECON GmbH";

	/*
	 * Backend-Api Controller
	 */
	public final static String BACKEND_API_URI = "wss://www1.fenecon.de:443/openems-backend2";

	/*
	 * System-Update.
	 */
	/**
	 * Name of the Debian package.
	 */
	public static final String SYSTEM_UPDATE_PACKAGE = "fems";
	public static final String SYSTEM_UPDATE_LATEST_VERSION_URL = "https://fenecon.de/fems-download/"
			+ SYSTEM_UPDATE_PACKAGE + "-latest.version";
	public static final String SYSTEM_UPDATE_SCRIPT_URL = "https://fenecon.de/fems-download/update-fems.sh";

	/*
	 * Energy management system.
	 */
	public static final String EMS_TAG = "fems";

}
