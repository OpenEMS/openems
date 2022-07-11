package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import io.openems.backend.metadata.odoo.Field.EdgeConfigUpdate;
import io.openems.common.types.EdgeConfigDiff;

public class InsertEdgeConfigUpdate extends DatabaseTask {

	private final Timestamp createDate;
	private final int odooId;
	private final String teaser;
	private final String details;

	public InsertEdgeConfigUpdate(int odooId, EdgeConfigDiff diff) {
		this.createDate = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC));
		this.odooId = odooId;
		this.teaser = diff.getAsText();
		this.details = diff.getAsHtml();
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psInsertEdgeConfigUpdate(connection);
		ps.setTimestamp(1, this.createDate);
		ps.setInt(2, this.odooId);
		ps.setString(3, this.teaser);
		ps.setString(4, this.details);
		ps.execute();
	}

	/**
	 * UPDATE {} SET openems_config = {}, openems_config_components = {} WHERE id =
	 * {};.
	 *
	 * @param connection the {@link Connection}
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psInsertEdgeConfigUpdate(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"INSERT INTO " + EdgeConfigUpdate.ODOO_TABLE //
						+ " (create_date" //
						+ ", " + EdgeConfigUpdate.DEVICE_ID.id() //
						+ ", " + EdgeConfigUpdate.TEASER.id() //
						+ ", " + EdgeConfigUpdate.DETAILS.id() + ")" //
						+ " VALUES(?, ?, ?, ?)");
	}

	@Override
	public String toString() {
		return "InsertEdgeConfigUpdate [createDate=" + this.createDate + ", odooId=" + this.odooId + ", teaser="
				+ this.teaser + "]";
	}

}