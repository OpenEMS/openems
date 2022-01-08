package io.openems.backend.timedata.influx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import org.influxdb.InfluxDBException.FieldTypeConflictException;
import org.influxdb.dto.Point.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;

/**
 * Handles Influx FieldTypeConflictExceptions. This helper provides conversion
 * functions to provide the correct field types for InfluxDB.
 */
public class FieldTypeConflictHandler {

	private static final Pattern FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN = Pattern.compile(
			"^partial write: field type conflict: input field \"(?<channel>.*)\" on measurement \"data\" is type (?<thisType>\\w+), already exists as type (?<requiredType>\\w+) dropped=\\d+$");

	private final Logger log = LoggerFactory.getLogger(FieldTypeConflictHandler.class);
	private final Influx parent;
	private final ConcurrentHashMap<String, BiConsumer<Builder, JsonElement>> specialCaseFieldHandlers = new ConcurrentHashMap<>();

	public FieldTypeConflictHandler(Influx parent) {
		this.parent = parent;
	}

	/**
	 * Handles a {@link FieldTypeConflictException}; adds special handling for
	 * fields that already exist in the database.
	 *
	 * @param e the {@link FieldTypeConflictException}
	 */
	public synchronized void handleException(FieldTypeConflictException e) {
		var matcher = FieldTypeConflictHandler.FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN.matcher(e.getMessage());
		if (!matcher.find()) {
			this.parent.logWarn(this.log, "Unable to add special field handler for message [" + e.getMessage() + "]");
			return;
		}
		var field = matcher.group("channel");
		var thisType = matcher.group("thisType");
		var requiredType = matcher.group("requiredType");

		if (this.specialCaseFieldHandlers.containsKey(field)) {
			// Special handling had already been added.
			return;
		}

		BiConsumer<Builder, JsonElement> handler = null;
		switch (requiredType) {
		case "string":
			handler = (builder, jValue) -> {
				var value = this.getAsFieldTypeString(jValue);
				if (value != null) {
					builder.addField(field, value);
				}
			};
			break;

		case "integer":
			handler = (builder, jValue) -> {
				try {
					var value = this.getAsFieldTypeNumber(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					try {
						// Failed -> try conversion to float and then to int
						var value = this.getAsFieldTypeFloat(jValue);
						if (value != null) {
							builder.addField(field, Math.round(value));
						}
					} catch (NumberFormatException e2) {
						this.parent.logWarn(this.log, "Unable to convert field [" + field + "] value [" + jValue
								+ "] to integer: " + e2.getMessage());
					}
				}
			};
			break;

		case "float":
			handler = (builder, jValue) -> {
				try {
					var value = this.getAsFieldTypeFloat(jValue);
					if (value != null) {
						builder.addField(field, value);
					}
				} catch (NumberFormatException e1) {
					this.parent.logInfo(this.log, "Unable to convert field [" + field + "] value [" + jValue
							+ "] to float: " + e1.getMessage());
				}
			};
			break;
		}

		if (handler == null) {
			this.parent.logWarn(this.log, "Unable to add special field handler for [" + field + "] from [" + thisType
					+ "] to [" + requiredType + "]");
		}
		this.parent.logInfo(this.log,
				"Add special field handler for [" + field + "] from [" + thisType + "] to [" + requiredType + "]");
		this.specialCaseFieldHandlers.put(field, handler);
	}

	/**
	 * Convert JsonElement to String.
	 *
	 * @param jValue the value
	 * @return the value as String; null if value represents null
	 */
	private String getAsFieldTypeString(JsonElement jValue) {
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
	private Number getAsFieldTypeNumber(JsonElement jValue) throws NumberFormatException {
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
	private Float getAsFieldTypeFloat(JsonElement jValue) throws NumberFormatException {
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
	public BiConsumer<Builder, JsonElement> getHandler(String field) {
		return this.specialCaseFieldHandlers.get(field);
	}
}
