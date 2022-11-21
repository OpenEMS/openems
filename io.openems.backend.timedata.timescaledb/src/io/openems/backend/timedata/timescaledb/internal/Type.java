package io.openems.backend.timedata.timescaledb.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Pattern;

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

public enum Type {
	INTEGER(1, "data_integer", "bigint" /* 8 bytes; covers Java byte, int and long */, //
			new String[] { "avg", "min", "max" }, ParseValueFromResultSet::integers, Subtract::integers), //
	FLOAT(2, "data_float", "double precision" /* 8 bytes; covers Java float and double */, //
			new String[] { "avg", "min", "max" }, ParseValueFromResultSet::floats, Subtract::floats), //
	STRING(3, "data_string", "text" /* variable-length character string */, //
			new String[] { "max" }, ParseValueFromResultSet::strings, Subtract::strings), //
	;

	public final int id;
	public final String sqlDataType;
	public final String tableRaw;
	public final String tableAggregate5m;
	public final String defaultAggregateFunction; // defaults to first aggregateFunction
	public final String[] aggregateFunctions;

	private final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet;
	private final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction;

	private Type(int id, String prefix, String sqlDataType, String[] aggregateFunctions,
			ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet,
			ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction) {
		this.id = id;
		this.sqlDataType = sqlDataType;
		this.tableRaw = prefix + "_raw";
		this.tableAggregate5m = prefix + "_5m";
		this.aggregateFunctions = aggregateFunctions;
		this.defaultAggregateFunction = aggregateFunctions[0];
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
					try {
						Double.parseDouble(s); // try parsing to Double
						return Type.FLOAT;
					} catch (NumberFormatException e) {
						return Type.STRING;
					}

				} else if (DETECT_INTEGER_PATTERN.test(s)) {
					try {
						Long.parseLong(s); // try parsing to Long
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

		private static final JsonElement integers(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getLong(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		private static final JsonElement floats(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getDouble(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}

		private static final JsonElement strings(ResultSet rs, Integer columnIndex) throws SQLException {
			var value = rs.getString(columnIndex);
			return rs.wasNull() ? JsonNull.INSTANCE : new JsonPrimitive(value);
		}
	}

	private static class Subtract {

		private static final JsonElement integers(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			Long a = JsonUtils.getAsType(OpenemsType.LONG, jA);
			Long b = JsonUtils.getAsType(OpenemsType.LONG, jB);
			if (a != null && b != null) {
				return new JsonPrimitive(a - b);
			}
			return JsonNull.INSTANCE;
		}

		private static final JsonElement floats(JsonElement jA, JsonElement jB) throws OpenemsNamedException {
			Double a = JsonUtils.getAsType(OpenemsType.DOUBLE, jA);
			Double b = JsonUtils.getAsType(OpenemsType.DOUBLE, jB);
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
	}
}