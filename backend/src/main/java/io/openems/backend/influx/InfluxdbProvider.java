package io.openems.backend.influx;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class InfluxdbProvider {

	private static Influxdb instance;

	/**
	 * Initialize InfluxDB object
	 *
	 * @param port
	 * @throws Exception
	 */
	public static void initialize(String database, String url, int port, String username, String password)
			throws Exception {
		if (database == null || url == null || username == null || password == null) {
			throw new Exception("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		InfluxdbProvider.instance = new Influxdb(database, url, port, username, password);
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized Influxdb getInstance() {
		return InfluxdbProvider.instance;
	}
}