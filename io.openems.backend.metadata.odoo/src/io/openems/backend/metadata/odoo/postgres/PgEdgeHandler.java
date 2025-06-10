package io.openems.backend.metadata.odoo.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Collectors;

import com.zaxxer.hikari.HikariDataSource;

import io.openems.backend.common.metadata.Metadata.GenericSystemLog;
import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeConfigUpdate;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.JsonFormat;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.utils.JsonUtils;

public final class PgEdgeHandler {

	private final HikariDataSource dataSource;

	protected PgEdgeHandler(HikariDataSource dataSource) {
		this.dataSource = dataSource;
	}

	/**
	 * Gets the {@link EdgeConfig} for an Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the {@link EdgeConfig}
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public EdgeConfig getEdgeConfig(String edgeId) throws SQLException, OpenemsNamedException {
		try (var con = this.dataSource.getConnection(); //
				var pst = con.prepareStatement(new StringBuilder() //
						.append("SELECT ").append(EdgeDevice.OPENEMS_CONFIG.id()) //
						.append(" FROM ").append(EdgeDevice.ODOO_TABLE) //
						.append(" WHERE name = ?") //
						.append(" LIMIT 1;") //
						.toString())) {
			pst.setString(1, edgeId);
			try (ResultSet rs = pst.executeQuery()) {
				while (rs.next()) {
					var string = rs.getString(1);
					if (string == null) {
						throw new OpenemsException("EdgeConfig for [" + edgeId + "] is null in the Database");
					}
					return EdgeConfig.fromJson(//
							JsonUtils.parseToJsonObject(string));
				}
			}
		}
		throw new OpenemsException("Unable to find EdgeConfig for [" + edgeId + "]");
	}

	/**
	 * Updates the {@link EdgeConfig} for an Edge-ID.
	 * 
	 * @param odooId     the Odoo-ID
	 * @param edgeConfig the {@link EdgeConfig}
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void updateEdgeConfig(int odooId, EdgeConfig edgeConfig) throws SQLException, OpenemsNamedException {
		try (var con = this.dataSource.getConnection(); //
				var pst = con.prepareStatement(new StringBuilder() //
						.append("UPDATE ").append(EdgeDevice.ODOO_TABLE) //
						.append(" SET ") //
						.append(EdgeDevice.OPENEMS_CONFIG.id()).append(" = ?, ") //
						.append(EdgeDevice.OPENEMS_CONFIG_COMPONENTS.id()).append(" = ?") //
						.append(" WHERE id = ?") //
						.toString())) {
			pst.setString(1, JsonUtils.prettyToString(edgeConfig.toJson()));
			pst.setString(2, JsonUtils.prettyToString(edgeConfig.componentsToJson(JsonFormat.WITHOUT_CHANNELS)));
			pst.setInt(3, odooId);
			pst.execute();
		}
	}

	/**
	 * Inserts an {@link EdgeConfigDiff} for an Edge-ID.
	 * 
	 * @param odooId the Odoo-ID
	 * @param diff   the {@link EdgeConfigDiff}
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void insertEdgeConfigUpdate(int odooId, EdgeConfigDiff diff) throws SQLException, OpenemsNamedException {
		try (var con = this.dataSource.getConnection(); //
				var pst = con.prepareStatement(new StringBuilder() //
						.append("INSERT INTO ").append(EdgeConfigUpdate.ODOO_TABLE) //
						.append(" (create_date") //
						.append(", ").append(EdgeConfigUpdate.DEVICE_ID.id()) //
						.append(", ").append(EdgeConfigUpdate.TEASER.id()) //
						.append(", ").append(EdgeConfigUpdate.DETAILS.id()) //
						.append(") VALUES(?, ?, ?, ?)") //
						.toString())) {
			pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
			pst.setInt(2, odooId);
			pst.setString(3, diff.getAsText());
			pst.setString(4, diff.getAsHtml());
			pst.execute();
		}
	}

	/**
	 * Inserts an {@link GenericSystemLog} for an Edge-ID.
	 * 
	 * @param odooId    the Odoo-ID
	 * @param systemLog the {@link GenericSystemLog}
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void insertGenericSystemLog(int odooId, GenericSystemLog systemLog)
			throws SQLException, OpenemsNamedException {
		try (var con = this.dataSource.getConnection(); //
				var pst = con.prepareStatement(new StringBuilder() //
						.append("INSERT INTO ").append(EdgeConfigUpdate.ODOO_TABLE) //
						.append(" (create_date") //
						.append(", ").append(EdgeConfigUpdate.DEVICE_ID.id()) //
						.append(", ").append(EdgeConfigUpdate.TEASER.id()) //
						.append(", ").append(EdgeConfigUpdate.DETAILS.id()) //
						.append(") VALUES(?, ?, ?, ?)") //
						.toString())) {
			pst.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC)));
			pst.setInt(2, odooId);
			pst.setString(3, systemLog.teaser());
			pst.setString(4, createHtml(systemLog));
			pst.execute();
		}
	}

	private static String createHtml(GenericSystemLog systemLog) {
		final var header = new StringBuilder();
		final var content = new StringBuilder();

		header.append("""
				<table border="1" style="border-collapse: collapse"\
				<thead>\
					<tr>""");
		content.append("<tr>");
		for (var entry : systemLog.getValues().entrySet()) {
			header.append("<th>%s</th>".formatted(entry.getKey()));
			content.append("<td>%s</td>".formatted(entry.getValue()));
		}
		header.append("<th>Executed By</th>");
		content.append("<td>%s: %s</td>".formatted(systemLog.user().getId(), systemLog.user().getName()));

		header.append("""
					</tr>
				</thead>
				<tbody>
				""");
		content.append("</tr>");

		return new StringBuilder() //
				.append(header) //
				.append(content) //
				.append("</tbody></table>") //
				.toString();
	}

	/**
	 * Updates the ProductType for an Edge-ID.
	 * 
	 * @param odooId      the Odoo-ID
	 * @param producttype the ProductType
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void updateProductType(int odooId, String producttype) throws SQLException, OpenemsNamedException {
		try (var con = this.dataSource.getConnection(); //
				var pst = con.prepareStatement(new StringBuilder() //
						.append("UPDATE ").append(EdgeDevice.ODOO_TABLE) //
						.append(" SET ") //
						.append(EdgeDevice.PRODUCTTYPE.id()).append(" = ?") //
						.append(" WHERE id = ?") //
						.toString())) {
			pst.setString(1, producttype);
			pst.setInt(2, odooId);
			pst.execute();
		}
	}

	/**
	 * Updates the OpenemsIsConnected field for multiple Edge-IDs.
	 * 
	 * @param odooIds     the Odoo-IDs
	 * @param isConnected true if online; false if offline
	 * @throws SQLException on error
	 */
	public void updateOpenemsIsConnected(Set<Integer> odooIds, boolean isConnected) throws SQLException {
		if (odooIds.isEmpty()) {
			return;
		}

		try (var con = this.dataSource.getConnection(); //
				var st = con.createStatement()) {
			st.executeUpdate(new StringBuilder() //
					.append("UPDATE ").append(EdgeDevice.ODOO_TABLE) //
					.append(" SET ").append(Field.EdgeDevice.OPENEMS_IS_CONNECTED.id()).append(" = ")
					.append(isConnected ? "TRUE" : "FALSE") //
					.append(" WHERE id IN (") //
					.append(odooIds.stream() //
							.map(String::valueOf) //
							.collect(Collectors.joining(","))) //
					.append(")") //
					.toString());
		}
	}

	/**
	 * Updates the LastMessage field for multiple Edge-IDs.
	 * 
	 * @param odooIds the Odoo-IDs
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void updateLastMessage(Set<Integer> odooIds) throws SQLException {
		if (odooIds.isEmpty()) {
			return;
		}

		try (var con = this.dataSource.getConnection(); //
				var st = con.createStatement()) {
			st.executeUpdate(new StringBuilder() //
					.append("UPDATE ").append(EdgeDevice.ODOO_TABLE) //
					.append(" SET ").append(Field.EdgeDevice.LASTMESSAGE.id()).append(" = (now() at time zone 'UTC')") //
					.append(" WHERE id IN (") //
					.append(odooIds.stream() //
							.map(String::valueOf) //
							.collect(Collectors.joining(","))) //
					.append(")") //
					.toString());
		}
	}

	/**
	 * Updates the Sum-State field for multiple Edge-IDs.
	 * 
	 * @param odooIds the Odoo-IDs
	 * @param level   the Sum-State {@link Level}
	 * @throws OpenemsNamedException on error
	 * @throws SQLException          on error
	 */
	public void updateSumState(Set<Integer> odooIds, Level level) throws SQLException {
		if (odooIds.isEmpty()) {
			return;
		}

		try (var con = this.dataSource.getConnection(); //
				var st = con.createStatement()) {
			st.executeUpdate(new StringBuilder() //
					.append("UPDATE ").append(EdgeDevice.ODOO_TABLE) //
					.append(" SET ").append(Field.EdgeDevice.OPENEMS_SUM_STATE.id()).append(" = '")
					.append(level.getName().toLowerCase()) //
					.append("' WHERE id IN (") //
					.append(odooIds.stream() //
							.map(String::valueOf) //
							.collect(Collectors.joining(","))) //
					.append(")") //
					.toString());
		}
	}
}