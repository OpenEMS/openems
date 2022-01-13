package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;

public class UpdateEdgeProducttype extends DatabaseTask {

	private final int odooId;
	private final String producttype;

	public UpdateEdgeProducttype(int odooId, String producttype) {
		this.odooId = odooId;
		this.producttype = producttype;
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psUpdateProductType(connection);
		ps.setString(1, this.producttype);
		ps.setInt(2, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET version = {} WHERE id = {};.
	 *
	 * @param connection the {@link Connection}
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateProductType(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.PRODUCT_TYPE.id() + " = ?" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateEdgeProducttype [odooId=" + this.odooId + ", producttype=" + this.producttype + "]";
	}

}