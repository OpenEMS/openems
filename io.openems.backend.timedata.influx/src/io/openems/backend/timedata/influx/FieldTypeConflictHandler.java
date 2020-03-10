package io.openems.backend.timedata.influx;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
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

	private final static Pattern FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN = Pattern.compile(
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
	 * @param failedPoints the failed points
	 * @param e            the {@link FieldTypeConflictException}
	 */
	public synchronized void handleException(FieldTypeConflictException e) {
		Matcher matcher = FIELD_TYPE_CONFLICT_EXCEPTION_PATTERN.matcher(e.getMessage());
		if (!matcher.find()) {
			this.parent.logWarn(this.log, "Unable to add special field handler for message [" + e.getMessage() + "]");
			return;
		}
		String field = matcher.group("channel");
		String thisType = matcher.group("thisType");
		String requiredType = matcher.group("requiredType");

		if (this.specialCaseFieldHandlers.containsKey(field)) {
			// Special handling had already been added.
			return;
		}

		BiConsumer<Builder, JsonElement> handler = null;
		switch (requiredType) {
		case "string":
			handler = (builder, jValue) -> {
				builder.addField(field, this.getAsFieldTypeString(jValue));
			};
			break;
		case "integer":
			handler = (builder, jValue) -> {
				try {
					builder.addField(field, this.getAsFieldTypeNumber(jValue));
				} catch (NumberFormatException e1) {
					try {
						// Failed -> try conversion to float and then to int
						builder.addField(field, Math.round(this.getAsFieldTypeFloat(jValue)));
					} catch (NumberFormatException e2) {
						this.parent.logWarn(this.log, "Unable to convert field [" + field + "] value [" + jValue
								+ "] to integer: " + e2.getMessage());
					}
				}
			};
			break;
		case "float":
			handler = (builder, jValue) -> {
				String value = jValue.toString().replace("\"", "");
				try {
					builder.addField(field, this.getAsFieldTypeFloat(jValue));
				} catch (NumberFormatException e1) {
					this.parent.logInfo(this.log, "Unable to convert field [" + field + "] value [" + value
							+ "] to float: " + e1.getMessage());
				}
			};
			break;
		}

		if (handler == null) {
			this.parent.logWarn(this.log, "Unable to add special field handler for [" + field + "] from [" + thisType
					+ "] to [" + requiredType + "]");
			return;
		} else {
			this.parent.logInfo(this.log,
					"Add special field handler for [" + field + "] from [" + thisType + "] to [" + requiredType + "]");
			this.specialCaseFieldHandlers.put(field, handler);
		}
	}

	/**
	 * Convert JsonElement to String
	 * 
	 * @param jValue the value
	 * @return the value as String
	 */
	private String getAsFieldTypeString(JsonElement jValue) {
		return jValue.toString().replace("\"", "");
	}

	/**
	 * Convert JsonElement to Number
	 * 
	 * @param jValue the value
	 * @return the value as Number
	 * @throws NumberFormatException on error
	 */
	private long getAsFieldTypeNumber(JsonElement jValue) throws NumberFormatException {
		String value = jValue.toString().replace("\"", "");
		try {
			return Long.parseLong(value);
		} catch (NumberFormatException e1) {
			if (value.equalsIgnoreCase("false")) {
				return 0l;
			} else if (value.equalsIgnoreCase("true")) {
				return 1l;
			} else {
				throw e1;
			}
		}
	}

	/**
	 * Convert JsonElement to Float
	 * 
	 * @param jValue the value
	 * @return the value as Float
	 * @throws NumberFormatException on error
	 */
	private float getAsFieldTypeFloat(JsonElement jValue) throws NumberFormatException {
		String value = jValue.toString().replace("\"", "");
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
