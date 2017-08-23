package io.openems.backend.odoo;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class Odoo {

	private static OdooSingleton instance;

	/**
	 * Initialize Odoo object
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initialize(String url, int port, String database, String username, String password)
			throws Exception {
		if (url == null || database == null || username == null || password == null) {
			throw new Exception("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		Odoo.instance = new OdooSingleton(url, port, database, username, password);
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized OdooSingleton instance() {
		return Odoo.instance;
	}
}