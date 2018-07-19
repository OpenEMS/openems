package io.openems.backend.timedata.influx;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class Utils {

	private final static Logger log = LoggerFactory.getLogger(Utils.class);

	/**
	 * Add value to Influx Builder in the correct data format
	 *
	 * @param builder
	 * @param channel
	 * @param value
	 * @return
	 */
	protected static Optional<Object> parseValue(String channel, Object value) {
		if (value == null) {
			return Optional.empty();
		}
		// convert JsonElement
		if (value instanceof JsonElement) {
			JsonElement jValueElement = (JsonElement) value;
			if (jValueElement.isJsonPrimitive()) {
				JsonPrimitive jValue = jValueElement.getAsJsonPrimitive();
				if (jValue.isNumber()) {
					try {
						// Avoid GSONs LazilyParsedNumber
						value = NumberFormat.getInstance().parse(jValue.toString());
					} catch (ParseException e) {
						log.error("Unable to parse Number: " + e.getMessage());
						value = jValue.getAsNumber();
					}
				} else if (jValue.isBoolean()) {
					value = jValue.getAsBoolean();
				} else if (jValue.isString()) {
					value = jValue.getAsString();
				}
			}
		}
		if (value instanceof Number) {
			Number numberValue = (Number) value;
			if (numberValue instanceof Integer) {
				return Optional.of(numberValue.intValue());
			} else if (numberValue instanceof Double) {
				return Optional.of(numberValue.doubleValue());
			} else {
				return Optional.of(numberValue);
			}
		} else if (value instanceof Boolean) {
			return Optional.of((Boolean) value);
		} else if (value instanceof String) {
			return Optional.of((String) value);
		}
		log.warn("Unknown type of value [" + value + "] channel [" + channel + "]. This should never happen.");
		return Optional.empty();
	}
}
