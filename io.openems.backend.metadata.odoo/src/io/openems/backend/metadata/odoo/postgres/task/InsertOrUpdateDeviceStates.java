package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;
import io.openems.common.channel.Level;
import io.openems.common.types.ChannelAddress;

public class InsertOrUpdateDeviceStates extends DatabaseTask {

	public static class DeviceState {
		private final ChannelAddress channelAddress;
		private final Level level;
		private final String stateChannelName;

		public DeviceState(ChannelAddress channelAddress, Level level, String stateChannelName) {
			this.channelAddress = channelAddress;
			this.level = level;
			this.stateChannelName = stateChannelName;
		}
	}

	private final int odooId;
	private final Timestamp timestamp;
	private final List<DeviceState> deviceStates;

	public InsertOrUpdateDeviceStates(int odooId, Timestamp timestamp, List<DeviceState> deviceStates) {
		this.odooId = odooId;
		this.timestamp = timestamp;
		this.deviceStates = deviceStates;
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psInsertOrUpdateDeviceState(connection);
		// device_id
		ps.setInt(1, this.odooId);
		// last_appearance
		ps.setTimestamp(2, this.timestamp);

		for (DeviceState deviceState : this.deviceStates) {
			// channel_address
			ps.setString(3, deviceState.channelAddress.toString());
			// level
			ps.setString(4, deviceState.level.name().toLowerCase());
			// component_id
			ps.setString(5, deviceState.channelAddress.getComponentId());
			// channel_name
			ps.setString(6, deviceState.stateChannelName);

			ps.execute();
		}
	}

	/**
	 * INSERT INTO {} (...) VALUES (...) ON CONFLICT (..) UPDATE SET
	 * item=excluded.item;
	 *
	 * <p>
	 * Be careful to synchronize access to the resulting PreparedStatement.
	 *
	 * @param connection the {@link Connection}
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psInsertOrUpdateDeviceState(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"INSERT INTO " + EdgeDeviceStatus.ODOO_TABLE //
						+ " (device_id, last_appearance, channel_address, level, component_id, channel_name)" //
						+ " VALUES(?, ?, ?, ?, ?, ?)" //
						+ "	ON CONFLICT (device_id, channel_address)" //
						+ " DO UPDATE SET" //
						+ " level=EXCLUDED.level,component_id=EXCLUDED.component_id,"//
						+ "channel_name=EXCLUDED.channel_name,last_appearance=EXCLUDED.last_appearance");
	}

	@Override
	public String toString() {
		return "InsertOrUpdateDeviceState [odooId=" + this.odooId + ", timestamp=" + this.timestamp + "]";
	}
}
