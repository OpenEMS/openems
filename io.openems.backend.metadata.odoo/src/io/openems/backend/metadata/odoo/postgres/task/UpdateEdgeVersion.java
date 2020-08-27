package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.types.SemanticVersion;

public class UpdateEdgeVersion extends DatabaseTask {

	private final int odooId;
	private final String version;

	public UpdateEdgeVersion(int odooId, SemanticVersion version) {
		this.odooId = odooId;
		this.version = version.toString();
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		PreparedStatement ps = this.psUpdateEdgeVersion(connection);
		ps.setString(1, this.version);
		ps.setInt(2, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET version = {} WHERE id = {};.
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateEdgeVersion(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_VERSION.id() + " = ?" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateEdgeVersion [odooId=" + this.odooId + ", version=" + this.version + "]";
	}

}