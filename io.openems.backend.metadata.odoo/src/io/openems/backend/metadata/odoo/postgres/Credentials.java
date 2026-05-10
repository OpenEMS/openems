package io.openems.backend.metadata.odoo.postgres;

import io.openems.backend.metadata.odoo.Config;

/**
 * Holds credentials for access to PostgreSQL.
 * 
 * @param host     the database host
 * @param port     the database port
 * @param user     the database user
 * @param password the database user password
 * @param database the database name
 */
public record Credentials(String host, int port, String user, String password, String database) {

	/**
	 * Creates {@link Credentials} from a {@link Config}uration.
	 *
	 * @param config the configuration
	 * @return a new {@link Credentials} object
	 */
	public static Credentials fromConfig(Config config) {
		return new Credentials(//
				config.pgHost(), config.pgPort(), config.pgUser(), config.pgPassword(), config.database());
	}
}
