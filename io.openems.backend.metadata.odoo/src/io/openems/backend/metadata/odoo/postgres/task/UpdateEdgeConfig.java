package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.gson.GsonBuilder;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.postgres.MyConnection;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.JsonFormat;

public class UpdateEdgeConfig implements DatabaseTask {

	private final int odooId;
	private final String fullConfig;
	private final String componentsConfig;

	public UpdateEdgeConfig(int odooId, EdgeConfig config) {
		this.odooId = odooId;
		this.fullConfig = new GsonBuilder().setPrettyPrinting().create().toJson(config.toJson());
		this.componentsConfig = new GsonBuilder().setPrettyPrinting().create()
				.toJson(config.componentsToJson(JsonFormat.WITHOUT_CHANNELS));
	}

	@Override
	public void execute(MyConnection connection) throws SQLException {
		PreparedStatement ps = this.psUpdateEdgeConfig(connection);
		ps.setString(1, this.fullConfig);
		ps.setString(2, this.componentsConfig);
		ps.setInt(3, this.odooId);
		ps.execute();
	}

	/**
	 * UPDATE {} SET openems_config = {}, openems_config_components = {} WHERE id =
	 * {};
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateEdgeConfig(MyConnection connection) throws SQLException {
		return connection.get().prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_CONFIG.id() + " = ?," //
						+ " " + EdgeDevice.OPENEMS_CONFIG_COMPONENTS.id() + " = ?" //
						+ " WHERE id = ?");
	}
}