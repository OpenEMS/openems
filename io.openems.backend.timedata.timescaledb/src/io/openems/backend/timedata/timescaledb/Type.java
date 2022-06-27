package io.openems.backend.timedata.timescaledb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.backend.timedata.timescaledb.Schema.ChannelMeta;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;

public enum Type {
	INTEGER(1, "data_integer", "bigint" /* 8 bytes; covers Java byte, int and long */, //
			new String[] { "avg", "min", "max" }, AddValueToStatement.INTEGER, ParseValueFromResultSet.INTEGER,
			Subtract.INTEGER), //
	FLOAT(2, "data_float", "double precision" /* 8 bytes; covers Java float and double */, //
			new String[] { "avg", "min", "max" }, AddValueToStatement.FLOAT, ParseValueFromResultSet.FLOAT,
			Subtract.FLOAT), //
	STRING(3, "data_string", "text" /* variable-length character string */, //
			new String[] { "max" }, AddValueToStatement.STRING, ParseValueFromResultSet.STRING, Subtract.STRING), //
	;

	private static final int IDX_TIME = 1;
	private static final int IDX_CHANNEL_ID = 2;
	private static final int IDX_VALUE = 3;

	public final int id;
	public final String sqlDataType;
	public final String tableRaw;
	public final String tableAggregate5m;
	public final String defaultAggregateFunction; // defaults to first aggregateFunction
	public final String[] aggregateFunctions;

	private final String sqlInsert;
	private final ThrowingBiConsumer<PreparedStatement, JsonElement, Exception> addValueToStatement;
	private final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet;
	private final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction;

	private Type(int id, String prefix, String sqlDataType, String[] aggregateFunctions,
			ThrowingBiConsumer<PreparedStatement, JsonElement, Exception> addValueToStatement,
			ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet,
			ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction) {
		this.id = id;
		this.sqlDataType = sqlDataType;
		this.tableRaw = prefix + "_raw";
		this.tableAggregate5m = prefix + "_5m";
		this.sqlInsert = "INSERT INTO " + this.tableRaw + " (time, channel_id, value) VALUES (?, ?, ?);";
		this.aggregateFunctions = aggregateFunctions;
		this.defaultAggregateFunction = aggregateFunctions[0];
		this.addValueToStatement = addValueToStatement;
		this.parseValueFromResultSet = parseValueFromResultSet;
		this.subtractFunction = subtractFunction;
	}

	/**
	 * Prepares a {@link PreparedStatement} for inserts to the 'raw' table of this
	 * {@link Type}.
	 * 
	 * @param con a database {@link Connection}
	 * @return the {@link PreparedStatement}
	 * @throws SQLException on error
	 */
	public PreparedStatement prepareStatement(Connection con) throws SQLException {
		return con.prepareStatement(this.sqlInsert);
	}

	/**
	 * Fills a {@link PreparedStatement} with data (timestamp, channel_id and
	 * value).
	 * 
	 * @param pst     the {@link PreparedStatement}; created via
	 *                {@link #prepareStatement(Connection)}
	 * @param point   the {@link Point} holding data
	 * @param channel the {@link ChannelMeta} object
	 * @throws Exception on error
	 */
	public void fillStatement(PreparedStatement pst, Point point, ChannelMeta channel) throws Exception {
		pst.setTimestamp(IDX_TIME, Timestamp.from(Instant.ofEpochMilli(point.timestamp)));
		pst.setInt(IDX_CHANNEL_ID, channel.id);
		this.addValueToStatement.accept(pst, point.value);
	}

	/**
	 * Parses a value from a {@link ResultSet} to {@link JsonElement}.
	 * 
	 * @param rs          the {@link ResultSet}
	 * @param columnIndex the first column is 1, the second is 2, ...
	 * @return a {@link JsonElement}
	 * @throws SQLException on error
	 */
	public JsonElement parseValueFromResultSet(ResultSet rs, int columnIndex) throws SQLException {
		return this.parseValueFromResultSet.apply(rs, columnIndex);
	}

	/**
	 * Subtracts two values.
	 * 
	 * @param minuend    the minuend of the subtraction
	 * @param subtrahend the subtrahend of the subtraction
	 * @return the result, possibly null
	 * @throws OpenemsNamedException on error
	 */
	public JsonElement subtract(JsonElement minuend, JsonElement subtrahend) throws OpenemsNamedException {
		return this.subtractFunction.apply(minuend, subtrahend);
	}

	/**
	 * Gets the {@link Type} from its ID.
	 * 
	 * @param id the ID
	 * @return the Type; null if unknown
	 */
	public static Type fromId(int id) {
		for (var type : Type.values()) {
			if (id == type.id) {
				return type;
			}
		}
		return null;
	}

	private static final Predicate<String> DETECT_INTEGER_PATTERN = //
			Pattern.compile("^[-+]?[0-9]+$").asPredicate();
	private static final Predicate<String> DETECT_FLOAT_PATTERN = //
			Pattern.compile("^[-+]?[0-9]*\\.[0-9]+$").asPredicate();

	/**
	 * Tries to detect the {@link Type} of a {@link JsonElement} value.
	 * 
	 * @param value the value
	 * @return the type
	 */
	public static Type detect(JsonElement value) {
		// Null is undetectable
		if (value.isJsonNull()) {
			return null;
		}
		// Handle known JsonPrimitive types
		if (value.isJsonPrimitive()) {
			var p = (JsonPrimitive) value;
			if (p.isNumber()) {
				// Numbers can be directly converted
				var n = p.getAsNumber();
				if (n.getClass().getName().equals("com.google.gson.internal.LazilyParsedNumber")) {
					// Avoid 'discouraged access'
					// LazilyParsedNumber stores value internally as String
					if (DETECT_FLOAT_PATTERN.test(n.toString())) {
						return Type.FLOAT;
					}
					return Type.INTEGER;

				} else if (n instanceof Integer || n instanceof Long || n instanceof Short || n instanceof Byte) {
					return Type.INTEGER;

				}
				return Type.FLOAT;

			} else if (p.isBoolean()) {
				// Booleans are converted to integer (0/1)
				return Type.INTEGER;

			} else if (p.isString()) {
				// Strings are parsed if they start with a number or minus
				var s = p.getAsString();
				if (DETECT_FLOAT_PATTERN.test(s)) {
					return Type.FLOAT;

				} else if (DETECT_INTEGER_PATTERN.test(s)) {
					return Type.INTEGER;
				}
				return Type.STRING;
			}
		}
		// TODO parse JsonObject and JsonArray
		return Type.STRING;
	}

	private static class AddValueToStatement {

		/**
		 * Parse a {@link JsonElement} value to 'bigint' and adds it to the
		 * {@link PreparedStatement}.
		 * 
		 * @param value the {@link JsonElement} value
		 */
		private static final ThrowingBiConsumer<PreparedStatement, JsonElement, Exception> INTEGER = (pst, json) -> {
			Long value = JsonUtils.getAsType(OpenemsType.LONG, json);
			if (value != null) {
				pst.setLong(IDX_VALUE, value);
			} else {
				pst.setObject(IDX_VALUE, null);
			}
		};

		/**
		 * Parse a {@link JsonElement} value to 'double precision' and adds it to the
		 * {@link PreparedStatement}.
		 * 
		 * @param value the {@link JsonElement} value
		 */
		private static final ThrowingBiConsumer<PreparedStatement, JsonElement, Exception> FLOAT = (pst, json) -> {
			Double value = JsonUtils.getAsType(OpenemsType.DOUBLE, json);
			if (value != null) {
				pst.setDouble(IDX_VALUE, value);
			} else {
				pst.setObject(IDX_VALUE, null);
			}
		};

		/**
		 * Parse a {@link JsonElement} value to 'text' and adds it to the
		 * {@link PreparedStatement}.
		 * 
		 * @param value the {@link JsonElement} value
		 */
		private static final ThrowingBiConsumer<PreparedStatement, JsonElement, Exception> STRING = (pst, json) -> {
			String value = JsonUtils.getAsType(OpenemsType.STRING, json);
			if (value != null) {
				pst.setString(IDX_VALUE, value);
			} else {
				pst.setObject(IDX_VALUE, null);
			}
		};
	}

	private static class ParseValueFromResultSet {

		private static final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> INTEGER = (rs,
				columnIndex) -> {
			var value = rs.getLong(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		};

		private static final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> FLOAT = (rs,
				columnIndex) -> {
			var value = rs.getDouble(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		};

		private static final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> STRING = (rs,
				columnIndex) -> {
			var value = rs.getString(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		};
	}

	private static class Subtract {

		private static final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> INTEGER = (
				jA, jB) -> {
			Long a = JsonUtils.getAsType(OpenemsType.LONG, jA);
			Long b = JsonUtils.getAsType(OpenemsType.LONG, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		};

		private static final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> FLOAT = (
				jA, jB) -> {
			Double a = JsonUtils.getAsType(OpenemsType.DOUBLE, jA);
			Double b = JsonUtils.getAsType(OpenemsType.DOUBLE, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		};

		private static final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> STRING = (
				jA, jB) -> {
			String a = JsonUtils.getAsType(OpenemsType.STRING, jA);
			if (a != null && !a.isBlank()) {
				return new JsonPrimitive(a);
			}
			String b = JsonUtils.getAsType(OpenemsType.STRING, jB);
			if (b != null && !b.isBlank()) {
				return new JsonPrimitive(b);
			}
			return JsonNull.INSTANCE;
		};
	}
}