package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.postgres.MyConnection;
import io.openems.common.types.SemanticVersion;

public class UpdateEdgeVersion implements DatabaseTask {

	private final int odooId;
	private final String version;

	public UpdateEdgeVersion(int odooId, SemanticVersion version) {
		this.odooId = odooId;
		this.version = version.toString();
	}

	@Override
	public void execute(MyConnection connection) throws SQLException {
		PreparedStatement ps = this.psUpdateEdgeVersion(connection);
		ps.setString(1, this.version);
		ps.setInt(2, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET version = {} WHERE id = {};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateEdgeVersion(MyConnection connection) throws SQLException {
		return connection.get().prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_VERSION.id() + " = ?" //
						+ " WHERE id = ?");
	}
}