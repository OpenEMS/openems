package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MyConnection {

	private final Logger log = LoggerFactory.getLogger(MyConnection.class);
	private final Credentials credentials;

	public MyConnection(Credentials credentials) {
		this.credentials = credentials;
	}

	private Connection conn;

	/**
	 * Returns the existing or creates a new Connection.
	 * 
	 * @return a database connection
	 * @throws SQLException on error
	 */
	public Connection get() throws SQLException {
		try {
			if (this.conn != null && !this.conn.isClosed()) {
				// Connection is already open
				return this.conn;
			}
		} catch (SQLException e) {
			this.log.warn(e.getMessage());
		}

		// Open new Database connection
		Properties props = new Properties();
		props.setProperty("user", this.credentials.getUser());
		if (this.credentials.getPassword() != null) {
			props.setProperty("password", this.credentials.getPassword());
		}

		if (!Driver.isRegistered()) {
			Driver.register();
		}
		String url = "jdbc:postgresql://" + this.credentials.getHost() + ":5432/" + this.credentials.getDatabase();
		this.conn = DriverManager.getConnection(url, props);
		this.conn.setAutoCommit(true);
		return this.conn;
	}

	/**
	 * Close the connection.
	 */
	public synchronized void deactivate() {
		if (this.conn != null) {
			try {
				this.conn.close();
			} catch (SQLException e) {
				this.log.warn(e.getMessage());
			}
		}
	}
}
