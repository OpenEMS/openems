package io.openems.backend.timedata.timescaledb.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import de.bytefish.pgbulkinsert.row.SimpleRow;
import de.bytefish.pgbulkinsert.row.SimpleRowWriter;
import io.openems.backend.timedata.timescaledb.internal.write.Point;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;

public enum Type {
	INTEGER(1, "data_integer", "integer" /* 4 bytes; covers Java boolean, byte and int */, //
			"avg", ParseValueFromResultSet::integers, Subtract::integers), //
	FLOAT(2, "data_float", "real" /* 4 bytes; covers Java float */, //
			"avg", ParseValueFromResultSet::floats, Subtract::floats), //
	STRING(3, "data_string", "text" /* variable-length character string */, //
			"max", ParseValueFromResultSet::strings, Subtract::strings), //
	LONG(4, "data_long", "bigint" /* 8 bytes; covers Java long */, //
			"avg", ParseValueFromResultSet::longs, Subtract::longs), //
	;

	public final int id;
	public final String sqlDataType;
	public final String rawTableName;
	public final String aggregate5mTableName;
	public final String aggregateFunction;

	private final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet;
	private final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction;

	private Type(int id, String prefix, String sqlDataType, String aggregateFunction,
			ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet,
			ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction) {
		this.id = id;
		this.sqlDataType = sqlDataType;
		this.rawTableName = prefix + "_raw";
		this.aggregate5mTableName = prefix + "_5m";
		this.aggregateFunction = aggregateFunction;
		this.parseValueFromResultSet = parseValueFromResultSet;
		this.subtractFunction = subtractFunction;
	}

	/**
	 * Fills a PgBulkInsert Row-Writer with data (timestamp, channel_id and value).
	 * 
	 * @param point the {@link Point} holding data
	 * @return a {@link Consumer} as required by
	 *         {@link SimpleRowWriter#startRow(Consumer)}
	 * @throws Exception on error
	 */
	public Consumer<SimpleRow> fillRow(Point point) {
		return row -> {
			row.setTimeStampTz(0 /* index of 'time' column */, point.timestamp);
			row.setInteger(1 /* index of 'channel_id' column */, point.channelId);
			point.addToSimpleRow(row, 2 /* index of 'value' column */);
		};
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
			if (type.id == id) {
				return type;
			}
		}
		return null;
	}

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
					if (StringUtils.matchesFloatPattern(n.toString())) {
						return Type.FLOAT;
					}
					var longValue = n.longValue();
					if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
						return Type.LONG;
					}
					return Type.INTEGER;

				} else if (n instanceof Integer || n instanceof Short || n instanceof Byte) {
					return Type.INTEGER;

				} else if (n instanceof Long) {
					return Type.LONG;
				}
				return Type.FLOAT;

			} else if (p.isBoolean()) {
				// Booleans are converted to integer (0/1)
				return Type.INTEGER;

			} else if (p.isString()) {
				// Strings are parsed if they start with a number or minus
				var s = p.getAsString();
				if (StringUtils.matchesFloatPattern(s)) {
					try {
						Double.parseDouble(s); // try parsing to Double
						return Type.FLOAT;
					} catch (NumberFormatException e) {
						return Type.STRING;
					}

				} else if (StringUtils.matchesIntegerPattern(s)) {
					try {
						var longValue = Long.parseLong(s); // try parsing to Long
						if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
							return Type.LONG;
						}
						return Type.INTEGER;
					} catch (NumberFormatException e) {
						return Type.STRING;
					}
				}
				return Type.STRING;
			}
		}
		// TODO parse JsonObject and JsonArray
		return Type.STRING;
	}

	private static class ParseValueFromResultSet {

		private ParseValueFromResultSet() {
		}

		private static final JsonElement integers(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getInt(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		private static final JsonElement floats(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getFloat(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		private static final JsonElement strings(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getString(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		private static final JsonElement longs(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getLong(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}
	}

	private static class Subtract {

		private Subtract() {
		}

		private static final JsonElement integers(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			Integer a = JsonUtils.getAsType(OpenemsType.INTEGER, jA);
			Integer b = JsonUtils.getAsType(OpenemsType.INTEGER, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		}

		private static final JsonElement floats(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			Double a = JsonUtils.getAsType(OpenemsType.FLOAT, jA);
			Double b = JsonUtils.getAsType(OpenemsType.FLOAT, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		}

		private static final JsonElement strings(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			String a = JsonUtils.getAsType(OpenemsType.STRING, jA);
			if (a != null && !a.isBlank()) {
				return new JsonPrimitive(a);
			}
			String b = JsonUtils.getAsType(OpenemsType.STRING, jB);
			if (b != null && !b.isBlank()) {
				return new JsonPrimitive(b);
			}
			return JsonNull.INSTANCE;
		}

		private static final JsonElement longs(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			Long a = JsonUtils.getAsType(OpenemsType.LONG, jA);
			Long b = JsonUtils.getAsType(OpenemsType.LONG, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		}
	}
}