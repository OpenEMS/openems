package io.openems.backend.metadata.odoo.postgres;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import org.postgresql.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.metadata.api.Metadata;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;
import io.openems.backend.metadata.odoo.MyEdge;
import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

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
	private PreparedStatement psInsertOrUpdateDeviceState() throws SQLException {
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

	private final Map<MyEdge, LocalDateTime> lastWriteDeviceStates = new HashMap<>();

	/**
	 * Updates the Device States table.
	 * 
	 * @param edge                the Edge
	 * @param activeStateChannels the active State-Channels
	 */
	public synchronized void writeDeviceStates(MyEdge edge, Map<ChannelAddress, Channel> activeStateChannels) {
		LocalDateTime lastWriteDeviceStates = this.lastWriteDeviceStates.get(edge);
		if (lastWriteDeviceStates != null && lastWriteDeviceStates.isAfter(LocalDateTime.now().minusMinutes(1))) {
			// do not write more often than once per minute
			return;
		}
		this.lastWriteDeviceStates.put(edge, LocalDateTime.now());

		try {
			PreparedStatement ps = this.psInsertOrUpdateDeviceState();
			for (Entry<ChannelAddress, Channel> entry : activeStateChannels.entrySet()) {
				ChannelDetail detail = entry.getValue().getDetail();
				if (!(detail instanceof ChannelDetailState)) {
					continue;
				}
				Level level = ((ChannelDetailState) detail).getLevel();
				ChannelAddress channelAddress = entry.getKey();
				Channel channel = entry.getValue();
				String stateChannelName;
				if (!channel.getText().isEmpty()) {
					stateChannelName = channel.getText();
				} else {
					stateChannelName = channel.getId();
				}
				// device_id
				ps.setInt(1, edge.getOdooId());
				// channel_address
				ps.setString(2, channelAddress.toString());
				// level
				ps.setString(3, level.name().toLowerCase());
				// component_id
				ps.setString(4, channelAddress.getComponentId());
				// channel_name
				ps.setString(5, stateChannelName);
				// last_appearance
				ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));

				ps.execute();
			}
		} catch (SQLException e) {
			this.log.error("Unable to update Device-States: " + e.getMessage() + "; for: "
					+ Metadata.activeStateChannelsToString(activeStateChannels));
		}
	}

}
