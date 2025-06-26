package io.openems.backend.metadata.odoo.postgres;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.odoo.OdooUtils;
import io.openems.common.exceptions.OpenemsException;

public class PgUtils {

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
	public static String getAsStringOrElse(ResultSet rs, Field field, String other) {
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
	public static int getAsInt(ResultSet rs, Field field) throws SQLException {
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
	public static Integer getAsIntegerOrElse(ResultSet rs, Field field, Integer other) {
		try {
			return PgUtils.getAsInt(rs, field);
		} catch (SQLException e) {
			return other;
		}
	}

	/**
	 * Return the Field of the ResultSet.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice field
	 * @return the value as ZonedDateTime
	 * @throws SQLException     SQL-Error
	 * @throws OpenemsException OpenEMS-Error
	 */
	public static ZonedDateTime getAsDate(ResultSet rs, Field field) throws SQLException, OpenemsException {
		var dateTimeStr = rs.getString(field.index());
		if (dateTimeStr != null) {
			// FIXME PgUtils should not use OdooUtils internally, but instead use Postgres
			// methods to handle 'timestamp' database columns
			return OdooUtils.DateTime.stringToDateTime(dateTimeStr);
		}
		throw new OpenemsException("Value of field [" + field.name() + "] is null.");
	}

	/**
	 * Return the Field of the ResultSet; or default value on error.
	 *
	 * @param rs    the ResultSet
	 * @param field the EdgeDevice field
	 * @param other the default value
	 * @return the value as ZonedDateTime
	 */
	public static ZonedDateTime getAsDateOrElse(ResultSet rs, Field field, ZonedDateTime other) {
		try {
			return PgUtils.getAsDate(rs, field);
		} catch (SQLException | OpenemsException e) {
			return other;
		}
	}
}
