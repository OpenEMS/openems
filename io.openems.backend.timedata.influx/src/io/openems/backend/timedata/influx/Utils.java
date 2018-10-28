package io.openems.backend.timedata.influx;

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
	protected static Optional<Object> parseValue(String channel, JsonElement jValueElement) {
		if (jValueElement == null) {
			return Optional.empty();
		}
		if (jValueElement.isJsonPrimitive()) {
			JsonPrimitive jValue = jValueElement.getAsJsonPrimitive();
			if (jValue.isNumber()) {
				try {
					return Optional.of(Long.parseLong(jValue.toString()));
				} catch (NumberFormatException e1) {
					try {
						return Optional.of(Double.parseDouble(jValue.toString()));
					} catch (NumberFormatException e2) {
						log.error("Unable to parse Number: " + e2.getMessage());
						return Optional.of(jValue.getAsNumber());
					}
				}
			} else if (jValue.isBoolean()) {
				return Optional.of(jValue.getAsBoolean());
			} else if (jValue.isString()) {
				return Optional.of(jValue.getAsString());
			} else {
				return Optional.of(jValueElement.toString());
			}
		} else {
			return Optional.of(jValueElement.toString());
		}
	}
}
