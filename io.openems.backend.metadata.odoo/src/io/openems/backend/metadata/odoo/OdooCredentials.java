package io.openems.backend.metadata.odoo;

/**
 * Holds credentials for access to Odoo.
 */
public class OdooCredentials {

	public static OdooCredentials fromConfig(Config config) {
		return new OdooCredentials(config.url(), config.database(), config.uid(), config.password());
	}

	private final String url;
	private final String database;
	private final int uid;
	private final String password;

	public OdooCredentials(String url, String database, int uid, String password) {
		this.url = url;
		this.database = database;
		this.uid = uid;
		this.password = password;
	}

	public int getUid() {
		return uid;
	}

	public String getDatabase() {
		return database;
	}

	public String getUrl() {
		return url;
	}

	public String getPassword() {
		return password;
	}
}
