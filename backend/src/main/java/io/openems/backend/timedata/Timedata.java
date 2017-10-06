package io.openems.backend.timedata;

import io.openems.backend.timedata.api.TimedataSingleton;
import io.openems.backend.timedata.dummy.TimedataDummySingleton;
import io.openems.backend.timedata.influx.InfluxdbSingleton;
import io.openems.common.exceptions.OpenemsException;

/**
 * Provider for Timedata singleton
 *
 * @author stefan.feilmeier
 *
 */
public class Timedata {

	private static TimedataSingleton instance = null;

	/**
	 * Initialize InfluxDB object
	 *
	 * @param port
	 * @throws Exception
	 */
	public static void initializeInfluxdb(String database, String url, int port, String username, String password)
			throws OpenemsException {
		if (database == null || url == null || username == null || password == null) {
			throw new OpenemsException("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		Timedata.instance = new InfluxdbSingleton(database, url, port, username, password);
	}

	/**
	 * Initialize Dummy provider
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initializeDummy() {
		Timedata.instance = new TimedataDummySingleton();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized TimedataSingleton instance() {
		return Timedata.instance;
	}
}