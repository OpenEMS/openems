package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.postgres.MyConnection;

public class UpdateEdgeProducttype implements DatabaseTask {

	private final int odooId;
	private final String producttype;

	public UpdateEdgeProducttype(int odooId, String producttype) {
		this.odooId = odooId;
		this.producttype = producttype;
	}

	@Override
	public void execute(MyConnection connection) throws SQLException {
		PreparedStatement ps = this.psUpdateProductType(connection);
		ps.setString(1, this.producttype);
		ps.setInt(2, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET version = {} WHERE id = {};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateProductType(MyConnection connection) throws SQLException {
		return connection.get().prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.PRODUCT_TYPE.id() + " = ?" //
						+ " WHERE id = ?");
	}
}