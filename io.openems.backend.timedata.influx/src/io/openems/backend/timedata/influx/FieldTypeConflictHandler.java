package io.openems.backend.timedata.influx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.influxdb.client.write.Point;
import com.influxdb.exceptions.InfluxException;

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
		this.initializePredefinedHandlers();
	}

	/**
	 * Add some already known Handlers.
	 */
	private void initializePredefinedHandlers() {
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

		var handler = this.createAndAddHandler(field, requiredType);

		if (handler == null) {
			this.parent.logWarn(this.log, "Unable to add special field handler for [" + field + "] from [" + thisType
					+ "] to [" + requiredType.name().toLowerCase() + "]");
		}
		this.parent.logInfo(this.log,
				"Add handler for [" + field + "] from [" + thisType + "] to [" + requiredType.name().toLowerCase()
						+ "]\n" //
						+ "Add predefined FieldTypeConflictHandler: this.createAndAddHandler(\"" + field
						+ "\", RequiredType." + requiredType.name() + ");");
		;

		return true;
	}

	private static enum RequiredType {
		STRING, INTEGER, FLOAT;
	}

	private BiConsumer<Point, JsonElement> createAndAddHandler(String field, RequiredType requiredType)
			throws IllegalStateException {
		var handler = this.createHandler(field, requiredType);
		if (this.specialCaseFieldHandlers.put(field, handler) != null) {
			throw new IllegalStateException("Handler for field [" + field + "] was already existing");
		}
		return handler;
	}

	/**
	 * Creates a Handler for the given field, to convert a Point to a
	 * 'requiredType'.
	 * 
	 * @param field        the field name, i.e. the Channel-Address
	 * @param requiredType the {@link RequiredType}
	 * @return the Handler
	 */
	private BiConsumer<Point, JsonElement> createHandler(String field, RequiredType requiredType) {
		switch (requiredType) {
		case STRING:
			return (builder, jValue) -> {
				var value = getAsFieldTypeString(jValue);
				if (value != null) {
					builder.addField(field, value);
				}
			};

		case INTEGER:
			return (builder, jValue) -> {
				try {
					var value = getAsFieldTypeNumber(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					try {
						// Failed -> try conversion to float and then to int
						var value = getAsFieldTypeFloat(jValue);
						if (value != null) {
							builder.addField(field, Math.round(value));
						}
					} catch (NumberFormatException e2) {
						this.parent.logWarn(this.log, "Unable to convert field [" + field + "] value [" + jValue
								+ "] to integer: " + e2.getMessage());
					}
				}
			};

		case FLOAT:
			return (builder, jValue) -> {
				try {
					var value = getAsFieldTypeFloat(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					this.parent.logInfo(this.log, "Unable to convert field [" + field + "] value [" + jValue
							+ "] to float: " + e1.getMessage());
				}
			};
		}
		return null; // can never happen
	}

	/**
	 * Convert JsonElement to String.
	 *
	 * @param jValue the value
	 * @return the value as String; null if value represents null
	 */
	private static String getAsFieldTypeString(JsonElement jValue) {
		if (jValue.isJsonNull()) {
			return null;
		}
		return jValue.toString().replace("\"", "");
	}

	/**
	 * Convert JsonElement to Number.
	 *
	 * @param jValue the value
	 * @return the value as Number; null if value represents null
	 * @throws NumberFormatException on error
	 */
	private static Number getAsFieldTypeNumber(JsonElement jValue) throws NumberFormatException {
		if (jValue.isJsonNull()) {
			return null;
		}
		var value = jValue.toString().replace("\"", "");
		if (value.isEmpty()) {
			return null;
		}
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e1) {
			if (value.equalsIgnoreCase("false")) {
				return 0L;
			} else if (value.equalsIgnoreCase("true")) {
				return 1L;
			} else {
				throw e1;
			}
		}
	}

	/**
	 * Convert JsonElement to Float.
	 *
	 * @param jValue the value
	 * @return the value as Float; null if value represents null
	 * @throws NumberFormatException on error
	 */
	private static Float getAsFieldTypeFloat(JsonElement jValue) throws NumberFormatException {
		if (jValue.isJsonNull()) {
			return null;
		}
		var value = jValue.toString().replace("\"", "");
		if (value.isEmpty()) {
			return null;
		}
		return Float.parseFloat(value);
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
