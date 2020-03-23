package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;
import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;

public class InsertOrUpdateDeviceState implements DatabaseTask {
	private final int odooId;
	private final ChannelAddress channelAddress;
	private final Level level;
	private final String stateChannelName;
	private final Timestamp timestamp;

	public InsertOrUpdateDeviceState(int odooId, ChannelAddress channelAddress, Level level, String stateChannelName,
			Timestamp timestamp) {
		this.odooId = odooId;
		this.channelAddress = channelAddress;
		this.level = level;
		this.stateChannelName = stateChannelName;
		this.timestamp = timestamp;
	}

	@Override
	public void execute(Connection connection) throws SQLException {
		PreparedStatement ps = this.psInsertOrUpdateDeviceState(connection);
		// device_id
		ps.setInt(1, this.odooId);
		// channel_address
		ps.setString(2, this.channelAddress.toString());
		// level
		ps.setString(3, this.level.name().toLowerCase());
		// component_id
		ps.setString(4, this.channelAddress.getComponentId());
		// channel_name
		ps.setString(5, this.stateChannelName);
		// last_appearance
		ps.setTimestamp(6, this.timestamp);

		ps.execute();
	}

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
	private PreparedStatement psInsertOrUpdateDeviceState(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"INSERT INTO " + EdgeDeviceStatus.ODOO_TABLE //
						+ " (device_id, channel_address, level, component_id, channel_name, last_appearance)" //
						+ " VALUES(?, ?, ?, ?, ?, ?)" //
						+ "	ON CONFLICT (device_id, channel_address)" //
						+ " DO UPDATE SET" //
						+ " level=EXCLUDED.level,component_id=EXCLUDED.component_id,"//
						+ "channel_name=EXCLUDED.channel_name,last_appearance=EXCLUDED.last_appearance");
	}

	@Override
	public String toString() {
		return "InsertOrUpdateDeviceState [odooId=" + odooId + ", channelAddress=" + channelAddress + ", level=" + level
				+ ", stateChannelName=" + stateChannelName + ", timestamp=" + timestamp + "]";
	}
}