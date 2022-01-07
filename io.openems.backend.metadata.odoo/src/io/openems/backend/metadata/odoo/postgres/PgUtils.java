package io.openems.backend.metadata.odoo.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.odoo.FieldValue;
import io.openems.common.exceptions.OpenemsException;

public class PgUtils {

	/**
	 * Adds a message in Odoo Chatter ('mail.thread').
	 *
	 * @param credentials the Odoo credentials
	 * @param model       Odoo model (e.g. 'res.partner')
	 * @param id          id of model
	 * @param message     the message
	 * @throws OpenemsException on error
	 */
	protected static void addChatterMessage(Credentials credentials, String model, int id, String message)
			throws OpenemsException {
	}

	/**
	 * Update a record in Odoo.
	 *
	 * @param credentials the Odoo credentials
	 * @param model       the Odoo model
	 * @param ids         ids of model to update
	 * @param fieldValues fields and values that should be written
	 * @throws OpenemsException on error
	 */
	public static void write(Credentials credentials, String model, Integer[] ids, FieldValue<?>... fieldValues)
			throws OpenemsException {
	}

	/**
	 * Return the Field of the ResultSet.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice
	 * @return the value as String
	 * @throws SQLException     on error
	 * @throws OpenemsException on null
	 */
	public static String getAsString(ResultSet rs, Field field) throws SQLException, OpenemsException {
		var result = rs.getString(field.index());
		if (result != null) {
			return result;
		}
		throw new OpenemsException("Value of field [" + field.name() + "] is null.");
	}

	/**
	 * Return the Field of the ResultSet; or default value on error.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice field
	 * @param other the default value
	 * @return the value as String
	 */
	public static String getAsStringOrElse(ResultSet rs, EdgeDevice field, String other) {
		try {
			return PgUtils.getAsString(rs, field);
		} catch (SQLException | OpenemsException e) {
			return other;
		}
	}

	/**
	 * Return the Field of the ResultSet.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice
	 * @return the value as Integer
	 * @throws SQLException on error
	 */
	public static int getAsInt(ResultSet rs, EdgeDevice field) throws SQLException {
		return rs.getInt(field.index());
	}

	/**
	 * Return the Field of the ResultSet; or default value on error.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice field
	 * @param other the default value
	 * @return the value as Integer
	 */
	public static Integer getAsIntegerOrElse(ResultSet rs, EdgeDevice field, Integer other) {
		try {
			return PgUtils.getAsInt(rs, field);
		} catch (SQLException e) {
			return other;
		}
	}
}
