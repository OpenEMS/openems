package io.openems.backend.timedata.influx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Longs;
import com.google.gson.JsonElement;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;

import io.openems.common.utils.JsonUtils;

/**
 * Handles Influx FieldTypeConflictExceptions. This helper provides conversion
 * functions to provide the correct field types for InfluxDB.
 */
public class FieldTypeConflictHandler {

	private static final Pattern FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN = Pattern.compile(
			"^.*partial write: field type conflict: input field \"(?<channel>.*)\" on measurement \"data\" is type (?<thisType>\\w+), already exists as type (?<requiredType>\\w+) dropped=\\d+$");

	private final Logger log = LoggerFactory.getLogger(FieldTypeConflictHandler.class);
	private final TimedataInfluxDb parent;
	private final ConcurrentHashMap<String, BiConsumer<Point, JsonElement>> specialCaseFieldHandlers = new ConcurrentHashMap<>();

	public FieldTypeConflictHandler(TimedataInfluxDb parent) {
		this.parent = parent;
	}

	/**
	 * Handles a {@link FieldTypeConflictException}; adds special handling for
	 * fields that already exist in the database.
	 *
	 * @param e the {@link FieldTypeConflictException}
	 */
	public synchronized void handleException(InfluxException e) throws IllegalStateException, IllegalArgumentException {
		this.handleExceptionMessage(e.getMessage());
	}

	protected synchronized boolean handleExceptionMessage(String message)
			throws IllegalStateException, IllegalArgumentException {
		var matcher = FieldTypeConflictHandler.FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN.matcher(message);
		if (!matcher.find()) {
			return false;
		}
		var field = matcher.group("channel");
		var thisType = matcher.group("thisType");
		var requiredType = RequiredType.valueOf(matcher.group("requiredType").toUpperCase());

		if (this.specialCaseFieldHandlers.containsKey(field)) {
			// Special handling had already been added.
			this.parent.logWarn(this.log, "Special field handler for message [" + message + "] is already existing");
			return false;
		}

		this.specialCaseFieldHandlers.computeIfAbsent(field, t -> this.createHandler(t, requiredType));

		this.parent.logInfo(this.log,
				"Add handler for [" + field + "] from [" + thisType + "] to [" + requiredType.name().toLowerCase()
						+ "]\n" //
						+ "Add predefined FieldTypeConflictHandler: this.createAndAddHandler(\"" + field
						+ "\", RequiredType." + requiredType.name() + ");");

		return true;
	}

	private static enum RequiredType {
		STRING, INTEGER, FLOAT;
	}

	/**
	 * Creates a Handler for the given field, to convert a Point to a
	 * 'requiredType'.
	 * 
	 * @param field        the field name, i.e. the Channel-Address
	 * @param requiredType the {@link RequiredType}
	 * @return the Handler
	 */
	protected BiConsumer<Point, JsonElement> createHandler(String field, RequiredType requiredType) {
		return switch (requiredType) {
		case STRING -> (builder, jValue) -> {
			var value = getAsFieldTypeString(jValue);
			if (value != null) {
				builder.addField(field, value);
			}
		};

		case INTEGER -> (builder, jValue) -> {
			final var value = getAsFieldTypeLong(jValue);
			if (value == null) {
				this.parent.logWarn(this.log,
						"Unable to convert field [" + field + "] value [" + jValue + "] to integer");
				return;
			}
			builder.addField(field, value);
		};

		case FLOAT -> (builder, jValue) -> {
			final var value = getAsFieldTypeDouble(jValue);
			if (value == null) {
				this.parent.logWarn(this.log,
						"Unable to convert field [" + field + "] value [" + jValue + "] to float");
				return;
			}
			builder.addField(field, value);
		};
		};
	}

	/**
	 * Convert JsonElement to String.
	 *
	 * @param jValue the value
	 * @return the value as String; null if value represents null
	 */
	protected static String getAsFieldTypeString(JsonElement jValue) {
		if (jValue.isJsonNull()) {
			return null;
		}
		return jValue.toString().replace("\"", "");
	}

	/**
	 * Convert JsonElement to Long.
	 *
	 * @param jValue the value
	 * @return the value as Long; null if value represents null
	 */
	protected static Long getAsFieldTypeLong(JsonElement jValue) {
		if (!jValue.isJsonPrimitive()) {
			return null;
		}
		if (JsonUtils.isNumber(jValue)) {
			return jValue.getAsNumber().longValue();
		}
		final var string = jValue.getAsString().replace("\"", "");

		final var longValue = Longs.tryParse(string);
		if (longValue != null) {
			return longValue;
		}

		final var doubleValue = Doubles.tryParse(string);
		if (doubleValue != null) {
			return doubleValue.longValue();
		}

		final var bool = tryParseToBooleanNumber(string);
		if (bool != null) {
			return bool.longValue();
		}

		return null;
	}

	/**
	 * Convert JsonElement to Double.
	 *
	 * @param jValue the value
	 * @return the value as Double; null if value represents null
	 */
	protected static Double getAsFieldTypeDouble(JsonElement jValue) {
		if (!jValue.isJsonPrimitive()) {
			return null;
		}
		if (JsonUtils.isNumber(jValue)) {
			return jValue.getAsNumber().doubleValue();
		}
		final var string = jValue.getAsString().replace("\"", "");

		final var doubleValue = Doubles.tryParse(string);
		if (doubleValue != null) {
			return doubleValue;
		}

		final var bool = tryParseToBooleanNumber(string);
		if (bool != null) {
			return bool.doubleValue();
		}

		return null;
	}

	private static Boolean tryParseBoolean(String value) {
		return switch (value) {
		case "true" -> true;
		case "false" -> false;
		default -> null;
		};
	}

	private static Integer tryParseToBooleanNumber(String value) {
		final var bool = tryParseBoolean(value);
		if (bool == null) {
			return null;
		}
		return bool ? 1 : 0;
	}

	/**
	 * Gets the handler for the given Field.
	 *
	 * @param field the Field
	 * @return the handler or null
	 */
	public BiConsumer<Point, JsonElement> getHandler(String field) {
		return this.specialCaseFieldHandlers.get(field);
	}
}
