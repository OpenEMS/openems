package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.google.gson.GsonBuilder;

import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.JsonFormat;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

public class UpdateEdgeConfig extends DatabaseTask {

	private final int odooId;
	private final String fullConfig;
	private final String componentsConfig;

	public UpdateEdgeConfig(int odooId, EdgeConfig config) {
		this.odooId = odooId;
		this.fullConfig = JsonUtils.prettyToString(config.toJson());
		this.componentsConfig = new GsonBuilder().setPrettyPrinting().create()
				.toJson(config.componentsToJson(JsonFormat.WITHOUT_CHANNELS));
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		var ps = this.psUpdateEdgeConfig(connection);
		ps.setString(1, this.fullConfig);
		ps.setString(2, this.componentsConfig);
		ps.setInt(3, this.odooId);
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
	private PreparedStatement psUpdateEdgeConfig(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_CONFIG.id() + " = ?," //
						+ " " + EdgeDevice.OPENEMS_CONFIG_COMPONENTS.id() + " = ?" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateEdgeConfig [odooId=" + this.odooId + ", componentsConfig="
				+ StringUtils.toShortString(this.componentsConfig, 100) + "]";
	}

}