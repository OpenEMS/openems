package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;

public class UpdateEdgeStateActive extends DatabaseTask {

	private final int odooId;

	public UpdateEdgeStateActive(int odooId) {
		this.odooId = odooId;
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psUpdateEdgeStateActive(connection);
		ps.setInt(1, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET state = 'active' WHERE id = {};.
	 *
	 * @param connection the {@link Connection}
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateEdgeStateActive(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.STATE.id() + " = 'active'" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateEdgeStateActive [odooId=" + this.odooId + "]";
	}

}