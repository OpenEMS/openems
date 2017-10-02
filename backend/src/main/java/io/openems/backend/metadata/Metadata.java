package io.openems.backend.metadata;

import io.openems.backend.metadata.api.MetadataSingleton;
import io.openems.backend.metadata.dummy.MetadataDummySingleton;
import io.openems.backend.metadata.odoo.OdooSingleton;
import io.openems.common.exceptions.OpenemsException;

/**
 * Provider for Metadata singleton
 *
 * @author stefan.feilmeier
 *
 */
public class Metadata {

	private static MetadataSingleton instance = null;

	/**
	 * Initialize Odoo object
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initializeOdoo(String url, int port, String database, String username,
			String password) throws OpenemsException {
		if (url == null || database == null || username == null || password == null) {
			throw new OpenemsException("Config missing: database [" + database + "], url [" + url + "], port [" + port
					+ "] username [" + username + "], password [" + password + "]");
		}
		Metadata.instance = new OdooSingleton(url, port, database, username, password);
	}

	/**
	 * Initialize Dummy provider
	 *
	 * @param port
	 * @throws Exception
	 */
	public static synchronized void initializeDummy() {
		Metadata.instance = new MetadataDummySingleton();
	}

	/**
	 * Returns the singleton instance
	 *
	 * @return
	 */
	public static synchronized MetadataSingleton instance() {
		return Metadata.instance;
	}
}