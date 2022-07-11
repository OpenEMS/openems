package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.channel.Level;

public class UpdateSumState extends DatabaseTask {

	private final int odooId;
	private final Level sumState;

	public UpdateSumState(int odooId, Level sumState) {
		this.odooId = odooId;
		this.sumState = sumState;
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psUpdateSumState(connection);
		final String sumStateString;
		if (this.sumState != null) {
			sumStateString = this.sumState.getName().toLowerCase();
		} else {
			sumStateString = "";
		}
		ps.setString(1, sumStateString);
		ps.setInt(2, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET openems_sum_state_level = {} WHERE id = {};.
	 *
	 * @param connection the {@link Connection}
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateSumState(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_SUM_STATE.id() + " = ?" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateSumState [odooId=" + this.odooId + ", sumState=" + this.sumState + "]";
	}

}