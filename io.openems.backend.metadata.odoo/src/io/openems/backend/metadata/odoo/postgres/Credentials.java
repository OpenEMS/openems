package io.openems.backend.metadata.odoo.postgres;

import io.openems.backend.metadata.odoo.Config;

/**
 * Holds credentials for access to PostgresQL.
 */
public class Credentials {

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

	private final String host;
	private final int port;
	private final String user;
	private final String password;
	private final String database;

	public Credentials(String host, int port, String user, String password, String database) {
		this.host = host;
		this.port = port;
		this.user = user;
		this.password = password;
		this.database = database;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public String getUser() {
		return this.user;
	}

	public String getPassword() {
		return this.password;
	}

	public String getDatabase() {
		return this.database;
	}
}
