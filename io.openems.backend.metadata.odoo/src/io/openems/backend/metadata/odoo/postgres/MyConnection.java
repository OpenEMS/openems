package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Properties;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;

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

		// Initialize everything
		this.psQueryAllEdges = null;
		this.psQueryEdgesWithApikey = null;
		this.psInsertOrUpdateDeviceState = null;
		this.psQueryNotAcknowledgedDeviceStates = null;

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

	private PreparedStatement psQueryAllEdges = null;

	/**
	 * SELECT {} FROM {edge.device};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	public PreparedStatement psQueryAllEdges() throws SQLException {
		Connection conn = this.get();
		if (this.psQueryAllEdges == null) {
			this.psQueryAllEdges = conn.prepareStatement(//
					"SELECT " + EdgeDevice.getSqlQueryFields() //
							+ " FROM " + EdgeDevice.ODOO_TABLE + ";");
		}
		return this.psQueryAllEdges;
	}

	private PreparedStatement psQueryEdgesWithApikey = null;

	/**
	 * SELECT {} FROM {edge.device} WHERE apikey = {};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	public PreparedStatement psQueryEdgesWithApikey() throws SQLException {
		Connection conn = this.get();
		if (this.psQueryEdgesWithApikey == null) {
			this.psQueryEdgesWithApikey = conn.prepareStatement(//
					"SELECT " + EdgeDevice.getSqlQueryFields() //
							+ " FROM " + EdgeDevice.ODOO_TABLE //
							+ " WHERE apikey = ?;");
		}

		return this.psQueryEdgesWithApikey;
	}

	private PreparedStatement psInsertOrUpdateDeviceState = null;

	/**
	 * INSERT INTO {} (...) VALUES (...) ON CONFLICT (..) UPDATE SET
	 * item=excluded.item;
	 * 
	 * <p>
	 * Be careful to synchronize access to the resulting PreparedStatement.
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	public PreparedStatement psInsertOrUpdateDeviceState() throws SQLException {
		Connection conn = this.get();
		if (this.psInsertOrUpdateDeviceState == null) {
			this.psInsertOrUpdateDeviceState = conn.prepareStatement(//
					"INSERT INTO " + EdgeDeviceStatus.ODOO_TABLE //
							+ " (device_id, channel_address, level, component_id, channel_name, last_appearance)" //
							+ " VALUES(?, ?, ?, ?, ?, ?)" //
							+ "	ON CONFLICT (device_id, channel_address)" //
							+ " DO UPDATE SET" //
							+ " level=EXCLUDED.level,component_id=EXCLUDED.component_id,"//
							+ "channel_name=EXCLUDED.channel_name,last_appearance=EXCLUDED.last_appearance");
		}
		return this.psInsertOrUpdateDeviceState;
	}

	private PreparedStatement psQueryNotAcknowledgedDeviceStates = null;

	/**
	 * SELECT level, component_id, channel_name FROM {} WHERE device_id = {} AND ...
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	public PreparedStatement psQueryNotAcknowledgedDeviceStates() throws SQLException {
		Connection conn = this.get();
		if (this.psQueryNotAcknowledgedDeviceStates == null) {
			this.psQueryNotAcknowledgedDeviceStates = conn.prepareStatement(//
					"SELECT level, component_id, channel_name" //
							+ " FROM " + EdgeDeviceStatus.ODOO_TABLE //
							+ " WHERE device_id = ?" //
							+ " AND (" //
							+ " (acknowledge_days > 0 AND last_appearance + interval '1 day' * acknowledge_days > last_acknowledge)" //
							+ " OR (acknowledge_days < 1 AND last_acknowledge IS NOT NULL)" //
							+ ")");
		}

		return this.psQueryNotAcknowledgedDeviceStates;
	}
}
