package io.openems.backend.odoo;

/**
 * Provider for OpenemsWebsocketServer singleton
 *
 * @author stefan.feilmeier
 *
 */
public class OdooProvider {

	private static Odoo instance;

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
		OdooProvider.instance = new Odoo(url, port, database, username, password);
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized Odoo getInstance() {
		return OdooProvider.instance;
	}
}