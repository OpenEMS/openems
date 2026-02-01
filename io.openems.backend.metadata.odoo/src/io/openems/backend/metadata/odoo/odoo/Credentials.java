package io.openems.backend.metadata.odoo.odoo;

import io.openems.backend.metadata.odoo.Config;

/**
 * Holds credentials for access to Odoo and PostgreSQL.
 * 
 * @param url      the connection URL
 * @param uid      the user ID
 * @param password the user password
 * @param database the database name
 */
public record Credentials(String url, int uid, String password, String database) {

	/**
	 * Creates {@link Credentials} from a {@link Config}uration.
	 *
	 * @param config the configuration
	 * @return a new {@link Credentials} object
	 */
	public static Credentials fromConfig(Config config) {
		return new Credentials(//
				config.odooProtocol(), config.odooHost(), config.odooPort(), config.odooUid(), config.odooPassword(),
				config.database());
	}

	public Credentials(Protocol protocol, String host, int port, int uid, String password, String database) {
		this(protocol.expression + "://" + host + ":" + port, uid, password, database);
	}
}
