package io.openems.backend.metadata.odoo.odoo;

import io.openems.backend.metadata.odoo.Config;

/**
 * Holds credentials for access to Odoo and PostgresQL.
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
				config.odooProtocol(), config.odooHost(), config.odooPort(), config.odooUid(), config.odooPassword(),
				config.database());
	}

	private final Protocol protocol;
	private final String host;
	private final int port;
	private final String url;
	private final int uid;
	private final String password;
	private final String database;

	public Credentials(Protocol protocol, String host, int port, int uid, String password, String database) {
		this.protocol = protocol;
		this.host = host;
		this.port = port;
		this.uid = uid;
		this.password = password;
		this.url = protocol.expression + "://" + host + ":" + port;
		this.database = database;
	}

	public Protocol getProtocol() {
		return this.protocol;
	}

	public String getHost() {
		return this.host;
	}

	public int getPort() {
		return this.port;
	}

	public int getUid() {
		return this.uid;
	}

	public String getUrl() {
		return this.url;
	}

	public String getPassword() {
		return this.password;
	}

	public String getDatabase() {
		return this.database;
	}
}
