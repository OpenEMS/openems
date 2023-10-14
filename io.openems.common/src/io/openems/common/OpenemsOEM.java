package io.openems.common;

/**
 * Adjustments for OpenEMS OEM distributions.
 */
// CHECKSTYLE:OFF
public class OpenemsOEM {
	// CHECKSTYLE:ON
	public enum Manufacturer {
		FENECON, //
		HECKERT; //
	}

	/*
	 * General.
	 */
	public static final String MANUFACTURER = "FENECON GmbH";

	/*
	 * Backend-Api Controller
	 */
	public static final String BACKEND_API_URI = "wss://www1.fenecon.de:443/openems-backend2";

	/*
	 * System-Update.
	 */
	/**
	 * Name of the Debian package.
	 */
	public static final String SYSTEM_UPDATE_PACKAGE = "fems";
	public static final String SYSTEM_UPDATE_SCRIPT_URL = "https://fenecon.de/fems-download/update-fems.sh";

	/**
	 * Gets the System-Update Latest-Version file URL.
	 * 
	 * @return the full URL
	 */
	public static String getSystemUpdateLatestVersionUrl() {
		return getSystemUpdateLatestVersionUrl(OpenemsConstants.VERSION_DEV_BRANCH);
	}

	protected static String getSystemUpdateLatestVersionUrl(String devBranch) {
		if (devBranch == null || devBranch.isBlank()) {
			return "https://fenecon.de/fems-download/" + SYSTEM_UPDATE_PACKAGE + "-latest.version";
		}
		return "https://dev.intranet.fenecon.de/" + devBranch + "/" + SYSTEM_UPDATE_PACKAGE + ".version";
	}

	/**
	 * Gets the parameters for the System-Update script.
	 * 
	 * @return parameters
	 */
	public static String getSystemUpdateScriptParams() {
		return getSystemUpdateScriptParams(OpenemsConstants.VERSION_DEV_BRANCH);
	}

	protected static String getSystemUpdateScriptParams(String devBranch) {
		if (devBranch == null || devBranch.isBlank()) {
			return "";
		}
		return " -fb \"" + devBranch + "\"";
	}

	/*
	 * Backend InfluxDB.
	 */
	public static final String INFLUXDB_TAG = "fems";

}
