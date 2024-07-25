package io.openems.backend.timedata.timescaledb.internal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
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
	private final String prefix;
	public final String defaultAggregateFunction; // defaults to first aggregateFunction
	public final String[] aggregateFunctions;

	private final Map<Priority, String> rawTableName = new EnumMap<>(Priority.class);
	private final Map<Priority, String> aggregate5mTableName = new EnumMap<>(Priority.class);

	private final ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet;
	private final ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction;

	private Type(int id, String prefix, String sqlDataType, String[] aggregateFunctions,
			ThrowingBiFunction<ResultSet, Integer, JsonElement, SQLException> parseValueFromResultSet,
			ThrowingBiFunction<JsonElement, JsonElement, JsonElement, OpenemsNamedException> subtractFunction) {
		this.id = id;
		this.sqlDataType = sqlDataType;
		this.prefix = prefix;
		this.aggregateFunctions = aggregateFunctions;
		this.defaultAggregateFunction = aggregateFunctions[0];
		this.parseValueFromResultSet = parseValueFromResultSet;
		this.subtractFunction = subtractFunction;
	}

	/**
	 * Gets the raw table name of the type and the specified priority.
	 * 
	 * @param priority the priority of the table
	 * @return the table name
	 */
	public String getRawTableName(Priority priority) {
		return this.rawTableName.computeIfAbsent(priority, t -> this.baseTableName(priority) + "_raw");
	}

	/**
	 * Gets the aggregate table name of the current type and the specified priority.
	 * 
	 * @param priority the priority of the table
	 * @return the table name
	 */
	public String getAggregate5mTableName(Priority priority) {
		return this.aggregate5mTableName.computeIfAbsent(priority, t -> this.baseTableName(priority) + "_5m");
	}

	private String baseTableName(Priority priority) {
		return this.prefix + "_" + priority.getTableSuffix();
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
					final var doubleValue = Doubles.tryParse(n.toString());
					if (doubleValue != null) {
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
				final var s = p.getAsString();

				// try to save string value as numbers
				final var longValue = Longs.tryParse(s);
				if (longValue != null) {
					return Type.INTEGER;
				}

				final var doubleValue = Doubles.tryParse(s);
				if (doubleValue != null) {
					return Type.FLOAT;
				}
			}
		}
		// TODO parse JsonObject and JsonArray
		return Type.STRING;
	}

	private static class ParseValueFromResultSet {

		private ParseValueFromResultSet() {
		}

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

		private Subtract() {
		}

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